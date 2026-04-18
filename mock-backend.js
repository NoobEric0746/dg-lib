#!/usr/bin/env node

const http = require('http');
const crypto = require('crypto');
const WebSocket = require('ws');

const PORT = parseInt(process.env.PORT || '9999', 10);
const HEARTBEAT_INTERVAL = parseInt(process.env.HEARTBEAT_INTERVAL || '60000', 10);
const DEFAULT_PUNISHMENT_TIME = parseInt(process.env.DEFAULT_PUNISHMENT_TIME || '1', 10);
const DEFAULT_PUNISHMENT_DURATION = parseInt(process.env.DEFAULT_PUNISHMENT_DURATION || '5', 10);
const MAX_MESSAGE_LENGTH = 1950;

const server = http.createServer();
const wss = new WebSocket.Server({ server, clientTracking: true });

const connections = new Map();
const pairings = new Map();
const reversePairings = new Map();
const socketIds = new WeakMap();
const pulseTasks = new Map();
let heartbeatTimer = null;

function log(level, message, extra) {
	const suffix = extra ? ` ${JSON.stringify(extra)}` : '';
	const text = `[mock-backend] [${level}] ${message}${suffix}`;
	if (level === 'ERROR') {
		console.error(text);
	} else if (level === 'WARN') {
		console.warn(text);
	} else {
		console.log(text);
	}
}

function uuid() {
	return crypto.randomUUID();
}

function sendJson(ws, payload) {
	if (!ws || ws.readyState !== WebSocket.OPEN) {
		return false;
	}
	ws.send(JSON.stringify(payload));
	return true;
}

function getClient(clientId) {
	return connections.get(clientId) || null;
}

function hasClient(clientId) {
	return connections.has(clientId);
}

function getPair(clientId) {
	return pairings.get(clientId) || reversePairings.get(clientId) || null;
}

function isPaired(clientId, targetId) {
	return pairings.get(clientId) === targetId || reversePairings.get(clientId) === targetId;
}

function clampStrength(value) {
	const number = Number.parseInt(value, 10);
	if (Number.isNaN(number)) {
		return 0;
	}
	return Math.max(0, Math.min(200, number));
}

function normalizeChannelNumber(channel) {
	return String(channel) === '2' ? 2 : 1;
}

function normalizeChannelName(channel) {
	return String(channel).toUpperCase() === 'B' ? 'B' : 'A';
}

function getRole(req) {
	const path = (req.url || '/').replace(/^\/+/, '').trim();
	return path ? 'app' : 'web';
}

function isOpen(ws) {
	return ws && ws.readyState === WebSocket.OPEN;
}

function sendError(ws, clientId, targetId, code) {
	sendJson(ws, {
		type: 'error',
		clientId: clientId || '',
		targetId: targetId || '',
		message: String(code)
	});
}

function sendBind(ws, clientId, targetId, code) {
	sendJson(ws, {
		type: 'bind',
		clientId: clientId || '',
		targetId: targetId || '',
		message: String(code)
	});
}

function sendBreak(ws, clientId, targetId) {
	sendJson(ws, {
		type: 'break',
		clientId: clientId || '',
		targetId: targetId || '',
		message: '209'
	});
}

function registerConnection(ws, req) {
	const clientId = uuid();
	const role = getRole(req);
	connections.set(clientId, {
		ws,
		role,
		createdAt: new Date(),
		lastHeartbeat: new Date(),
		strengthA: 0,
		strengthB: 0,
		limitA: 100,
		limitB: 100
	});
	socketIds.set(ws, clientId);

	sendBind(ws, clientId, '', 'targetId');
	log('INFO', 'client connected', {
		clientId,
		role,
		path: req.url || '/',
		remote: req.socket.remoteAddress || 'unknown'
	});
	return clientId;
}

function clearPulseTask(key) {
	const task = pulseTasks.get(key);
	if (!task) {
		return;
	}
	clearInterval(task.intervalId);
	pulseTasks.delete(key);
}

function clearPulseTasksForWebClient(webClientId) {
	for (const key of Array.from(pulseTasks.keys())) {
		if (key.startsWith(`${webClientId}:`)) {
			clearPulseTask(key);
		}
	}
}

function validateEnvelope(rawText) {
	let data;
	try {
		data = JSON.parse(rawText);
	} catch (err) {
		return { valid: false, code: '403', reason: 'invalid json' };
	}

	if (!data || typeof data !== 'object' || Array.isArray(data)) {
		return { valid: false, code: '403', reason: 'payload not object' };
	}

	return { valid: true, data };
}

function validateSource(clientId, ws) {
	const client = getClient(clientId);
	return !!client && client.ws === ws;
}

function validatePayload(boundClientId, data) {
	if (!Object.prototype.hasOwnProperty.call(data, 'type')) {
		return { valid: false, code: '403', reason: 'missing type' };
	}

	if (typeof data.clientId !== 'string' || !data.clientId) {
		return { valid: false, code: '404', reason: 'missing clientId' };
	}

	if (typeof data.targetId !== 'string') {
		return { valid: false, code: '404', reason: 'missing targetId' };
	}

	if (data.clientId !== boundClientId && data.targetId !== boundClientId) {
		return { valid: false, code: '404', reason: 'source mismatch' };
	}

	if (data.type === 'bind') {
		if (!data.targetId) {
			return { valid: false, code: '210', reason: 'missing targetId' };
		}
		return { valid: true };
	}

	if (!data.targetId) {
		return { valid: false, code: '404', reason: 'missing targetId' };
	}

	if (typeof data.message !== 'string' || !data.message) {
		return { valid: false, code: '404', reason: 'missing message' };
	}

	if (data.message.length > MAX_MESSAGE_LENGTH) {
		return { valid: false, code: '405', reason: 'message too long' };
	}

	return { valid: true };
}

function pairClients(webClientId, appClientId) {
	if (!hasClient(webClientId) || !hasClient(appClientId)) {
		return { success: false, code: '401' };
	}

	const webClient = getClient(webClientId);
	const appClient = getClient(appClientId);
	if (!webClient || !appClient || webClient.role !== 'web' || appClient.role !== 'app') {
		return { success: false, code: '401' };
	}

	if (pairings.has(webClientId) || reversePairings.has(appClientId)) {
		return { success: false, code: '400' };
	}

	pairings.set(webClientId, appClientId);
	reversePairings.set(appClientId, webClientId);

	sendBind(webClient.ws, webClientId, appClientId, '200');
	sendBind(appClient.ws, webClientId, appClientId, '200');
	log('INFO', 'paired clients', { webClientId, appClientId });
	return { success: true, code: '200' };
}

function applyStrengthState(appClientId, message) {
	const appClient = getClient(appClientId);
	if (!appClient) {
		return false;
	}

	const raw = message.slice('strength-'.length);
	const parts = raw.split('+');
	if (parts.length < 3) {
		return false;
	}

	const channel = normalizeChannelNumber(parts[0]);
	const mode = Number.parseInt(parts[1], 10);
	const value = clampStrength(parts[2]);

	const currentKey = channel === 2 ? 'strengthB' : 'strengthA';
	const limitKey = channel === 2 ? 'limitB' : 'limitA';
	const currentValue = appClient[currentKey];
	const limitValue = appClient[limitKey];

	if (mode === 0) {
		appClient[currentKey] = Math.max(0, currentValue - value);
	} else if (mode === 1) {
		appClient[currentKey] = Math.min(limitValue, currentValue + value);
	} else if (mode === 2) {
		appClient[currentKey] = Math.min(limitValue, value);
	} else {
		return false;
	}

	return true;
}

function sendStrengthStateToWeb(webClientId, appClientId) {
	const webClient = getClient(webClientId);
	const appClient = getClient(appClientId);
	if (!webClient || !appClient) {
		return;
	}

	sendJson(webClient.ws, {
		type: 'msg',
		clientId: webClientId,
		targetId: appClientId,
		message: `strength-${appClient.strengthA}+${appClient.strengthB}+${appClient.limitA}+${appClient.limitB}`
	});
}

function forwardToApp(webClientId, appClientId, message) {
	const appClient = getClient(appClientId);
	if (!appClient || !isOpen(appClient.ws)) {
		return { success: false, code: '404' };
	}

	sendJson(appClient.ws, {
		type: 'msg',
		clientId: webClientId,
		targetId: appClientId,
		message
	});
	return { success: true, code: '200' };
}

function routeFromWeb(data) {
	const webClientId = data.clientId;
	const appClientId = data.targetId;

	if (!isPaired(webClientId, appClientId)) {
		return { success: false, code: '402' };
	}

	if ([1, 2, 3, '1', '2', '3'].includes(data.type)) {
		const type = Number.parseInt(data.type, 10);
		const channel = normalizeChannelNumber(data.channel);
		const strength = clampStrength(data.strength);
		const mode = type - 1;
		const amount = type >= 3 ? strength : (strength || 1);
		const message = `strength-${channel}+${mode}+${amount}`;
		const result = forwardToApp(webClientId, appClientId, message);
		if (!result.success) {
			return result;
		}
		if (!applyStrengthState(appClientId, message)) {
			return { success: false, code: '403' };
		}
		sendStrengthStateToWeb(webClientId, appClientId);
		return { success: true, code: '200' };
	}

	if (data.type === 4 || data.type === '4') {
		const result = forwardToApp(webClientId, appClientId, data.message);
		if (!result.success) {
			return result;
		}
		if (data.message.startsWith('strength-')) {
			if (!applyStrengthState(appClientId, data.message)) {
				return { success: false, code: '403' };
			}
			sendStrengthStateToWeb(webClientId, appClientId);
		}
		if (data.message.startsWith('clear-')) {
			clearPulseTask(`${webClientId}:${data.message === 'clear-2' ? 'B' : 'A'}`);
		}
		return { success: true, code: '200' };
	}

	if (data.type === 'clientMsg') {
		if (!data.channel) {
			return { success: false, code: '406' };
		}

		const appClient = getClient(appClientId);
		if (!appClient || !isOpen(appClient.ws)) {
			return { success: false, code: '404' };
		}

		const channel = normalizeChannelName(data.channel);
		const prefix = `${channel}:`;
		if (!data.message.startsWith(prefix)) {
			return { success: false, code: '403' };
		}

		const key = `${webClientId}:${channel}`;
		const totalSends = Math.max(1, DEFAULT_PUNISHMENT_TIME * Math.max(1, Number.parseInt(data.time, 10) || DEFAULT_PUNISHMENT_DURATION));
		const intervalMs = Math.max(50, Math.floor(1000 / DEFAULT_PUNISHMENT_TIME));
		const pulsePayload = {
			type: 'msg',
			clientId: webClientId,
			targetId: appClientId,
			message: `pulse-${data.message}`
		};

		if (pulseTasks.has(key)) {
			clearPulseTask(key);
			sendJson(appClient.ws, {
				type: 'msg',
				clientId: webClientId,
				targetId: appClientId,
				message: `clear-${channel === 'B' ? '2' : '1'}`
			});
		}

		let remaining = totalSends;
		const sendOnce = () => {
			if (!isOpen(appClient.ws)) {
				clearPulseTask(key);
				return;
			}
			sendJson(appClient.ws, pulsePayload);
			remaining -= 1;
			if (remaining <= 0) {
				clearPulseTask(key);
			}
		};

		sendOnce();
		if (remaining > 0) {
			pulseTasks.set(key, { intervalId: setInterval(sendOnce, intervalMs) });
		}
		return { success: true, code: '200' };
	}

	return { success: false, code: '403' };
}

function routeFromApp(data) {
	const appClientId = data.targetId;
	const webClientId = data.clientId;

	if (!isPaired(webClientId, appClientId)) {
		return { success: false, code: '402' };
	}

	const webClient = getClient(webClientId);
	if (!webClient || !isOpen(webClient.ws)) {
		return { success: false, code: '404' };
	}

	sendJson(webClient.ws, {
		type: data.type,
		clientId: webClientId,
		targetId: appClientId,
		message: data.message
	});
	return { success: true, code: '200' };
}

function disconnectClient(clientId, reason) {
	const client = getClient(clientId);
	if (!client) {
		return;
	}

	const pairId = getPair(clientId);
	if (pairId) {
		const pairClient = getClient(pairId);
		if (pairClient) {
			const webClientId = client.role === 'web' ? clientId : pairId;
			const appClientId = client.role === 'web' ? pairId : clientId;
			sendBreak(pairClient.ws, webClientId, appClientId);
		}

		if (client.role === 'web') {
			pairings.delete(clientId);
			reversePairings.delete(pairId);
			clearPulseTasksForWebClient(clientId);
		} else {
			reversePairings.delete(clientId);
			pairings.delete(pairId);
			clearPulseTasksForWebClient(pairId);
		}
	}

	connections.delete(clientId);
	log('INFO', 'client disconnected', { clientId, reason: reason || 'closed' });
}

function ensureHeartbeat() {
	if (heartbeatTimer) {
		return;
	}
	heartbeatTimer = setInterval(() => {
		for (const [clientId, client] of connections.entries()) {
			if (!isOpen(client.ws)) {
				continue;
			}
			client.lastHeartbeat = new Date();
			sendJson(client.ws, {
				type: 'heartbeat',
				clientId,
				targetId: getPair(clientId) || '',
				message: '200'
			});
		}
	}, HEARTBEAT_INTERVAL);
}

wss.on('connection', (ws, req) => {
	const boundClientId = registerConnection(ws, req);
	ensureHeartbeat();

	ws.on('message', (rawMessage) => {
		const text = rawMessage.toString('utf8');
		const parsed = validateEnvelope(text);
		if (!parsed.valid) {
			sendError(ws, '', '', parsed.code);
			return;
		}

		const data = parsed.data;
		const validation = validatePayload(boundClientId, data);
		if (!validation.valid) {
			if (data.type === 'bind') {
				sendBind(ws, data.clientId || '', data.targetId || '', validation.code);
			} else {
				sendError(ws, data.clientId || '', data.targetId || '', validation.code);
			}
			return;
		}

		const sourceMatchesClientId = validateSource(data.clientId, ws);
		const sourceMatchesTargetId = validateSource(data.targetId, ws);
		if (!sourceMatchesClientId && !sourceMatchesTargetId) {
			sendError(ws, data.clientId || '', data.targetId || '', '404');
			return;
		}

		let result;
		if (data.type === 'bind') {
			result = pairClients(data.clientId, data.targetId);
			if (!result.success) {
				sendBind(ws, data.clientId, data.targetId, result.code);
			}
			return;
		}

		if (sourceMatchesClientId) {
			result = routeFromWeb(data);
		} else {
			result = routeFromApp(data);
		}

		if (!result.success) {
			sendError(ws, data.clientId || '', data.targetId || '', result.code);
		}
	});

	ws.on('close', (code, reason) => {
		disconnectClient(boundClientId, `${code}:${String(reason || '')}`);
	});

	ws.on('error', (err) => {
		log('ERROR', 'socket error', { clientId: boundClientId, error: err.message });
		const pairId = getPair(boundClientId);
		if (pairId) {
			const pairClient = getClient(pairId);
			if (pairClient) {
				const webClientId = getClient(boundClientId)?.role === 'web' ? boundClientId : pairId;
				const appClientId = getClient(boundClientId)?.role === 'web' ? pairId : boundClientId;
				sendError(pairClient.ws, webClientId, appClientId, '500');
			}
		}
	});
});

server.listen(PORT, () => {
	log('INFO', 'DG-LAB standard backend ready', {
		port: PORT,
		heartbeatInterval: HEARTBEAT_INTERVAL,
		defaultPunishmentTime: DEFAULT_PUNISHMENT_TIME,
		defaultPunishmentDuration: DEFAULT_PUNISHMENT_DURATION
	});
	log('INFO', 'connection roles: `/` for web frontend, `/<anything>` for app clients');
});

function shutdown(signal) {
	log('INFO', 'shutting down', { signal });
	if (heartbeatTimer) {
		clearInterval(heartbeatTimer);
		heartbeatTimer = null;
	}
	for (const key of Array.from(pulseTasks.keys())) {
		clearPulseTask(key);
	}
	for (const client of connections.values()) {
		try {
			client.ws.close(1001, 'server shutdown');
		} catch (err) {
			log('WARN', 'close failed', { error: err.message });
		}
	}
	wss.close(() => {
		server.close(() => process.exit(0));
	});
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));
