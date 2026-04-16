#!/usr/bin/env node
/**
 * Mock DG-LAB WebSocket Backend for Testing
 * 模拟 DG-LAB v2 WebSocket 后端，用于本地测试 Forge 模组
 */

const WebSocket = require('ws');
const http = require('http');
const url = require('url');

const PORT = 9999;
const server = http.createServer();
const wss = new WebSocket.Server({ server });

console.log(`🚀 DG-LAB Mock Backend starting on port ${PORT}...`);

const clients = new Map(); // clientId -> { ws, targetId, strengths }

wss.on('connection', (ws, req) => {
  const clientPath = url.parse(req.url).pathname;
  const clientId = clientPath.replace('/', '');
  
  console.log(`📱 New connection: clientId=${clientId}`);
  
  // 为新连接分配 targetId（简化：使用随机字符串）
  const targetId = Math.random().toString(36).substring(2, 10);
  
  clients.set(clientId, {
    ws,
    targetId,
    strengthA: 0,
    strengthB: 0,
  });
  
  // 发送初始 bind 消息给新连接
  const bindMsg = {
    type: 'bind',
    clientId: clientId,
    targetId: '',
    message: 'targetId'
  };
  ws.send(JSON.stringify(bindMsg));
  console.log(`  ↳ Sent initial bind (clientId assignment)`);
  
  // 模拟 APP 连接并配对（延迟 2 秒后自动配对）
  setTimeout(() => {
    if (clients.has(clientId) && clients.get(clientId).ws === ws) {
      const pairMsg = {
        type: 'bind',
        clientId: clientId,
        targetId: targetId,
        message: '200'
      };
      ws.send(JSON.stringify(pairMsg));
      console.log(`  ✅ Sent pairing bind (code=200, targetId=${targetId})`);
    }
  }, 2000);
  
  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString());
      console.log(`📥 Received from ${clientId}:`, msg.type, msg.message);
      
      // 解析强度指令
      if (msg.type === 'msg' && msg.message && msg.message.startsWith('strength-')) {
        const parts = msg.message.split('+');
        if (parts.length >= 1) {
          console.log(`  💪 Strength update: ${msg.message}`);
        }
      } else if ([1, 2, 3, 4].includes(msg.type)) {
        // 控制指令
        console.log(`  🎮 Control command: type=${msg.type}`);
        
        // 模拟回复强度状态
        const statusMsg = {
          type: 'msg',
          clientId: msg.clientId,
          targetId: msg.targetId,
          message: 'strength-10+20+100+100' // A: 10/100, B: 20/100
        };
        ws.send(JSON.stringify(statusMsg));
        console.log(`  ↳ Sent strength feedback`);
      }
    } catch (ex) {
      console.warn(`  ⚠️  Failed to parse message:`, ex.message);
    }
  });
  
  ws.on('close', () => {
    console.log(`❌ Connection closed: clientId=${clientId}`);
    clients.delete(clientId);
  });
  
  ws.on('error', (err) => {
    console.error(`⚠️  WebSocket error for ${clientId}:`, err.message);
  });
});

server.listen(PORT, () => {
  console.log(`✨ Mock Backend ready at ws://127.0.0.1:${PORT}`);
  console.log(`📋 Features:`);
  console.log(`   - Accepts connections and assigns clientId`);
  console.log(`   - Auto-pairs after 2 seconds (simulates APP)`);
  console.log(`   - Echoes strength commands`);
  console.log(`   - Press Ctrl+C to exit\n`);
});

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\n👋 Shutting down...');
  wss.clients.forEach(client => client.close());
  server.close(() => {
    console.log('Done!');
    process.exit(0);
  });
});
