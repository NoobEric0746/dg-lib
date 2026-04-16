import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

public class test_qr_generation {
    public static void main(String[] args) {
        try {
            String wsUrl = "ws://127.0.0.1:9999/testclient123";
            String qrContent = "https://www.dungeon-lab.com/app-download.php#DGLAB-SOCKET#" + wsUrl;
            
            System.out.println("Testing QR Code Generation");
            System.out.println("QR Content: " + qrContent);
            System.out.println("QR Content Length: " + qrContent.length());
            
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 200, 200);
            
            System.out.println("✅ QR Code generated successfully!");
            System.out.println("Matrix size: " + matrix.getWidth() + "x" + matrix.getHeight());
        } catch (Exception e) {
            System.err.println("❌ Failed to generate QR code:");
            e.printStackTrace();
        }
    }
}
