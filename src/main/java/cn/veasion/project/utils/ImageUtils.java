package cn.veasion.project.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * ImageUtils
 *
 * @author luozhuowei
 * @date 2022/4/15
 */
public class ImageUtils {

    /**
     * 二维码解码
     */
    public static String decodeQRImage(File qrFile) throws Exception {
        BufferedImage image = ImageIO.read(qrFile);
        int width = image.getWidth();
        int height = image.getHeight();
        LuminanceSource source = new RGBLuminanceSource(width, height, image.getRGB(0, 0, width, height, null, 0, width));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
        return new MultiFormatReader().decode(bitmap, hints).getText();
    }

    /**
     * 生成二维码（base64图片字符串，纯数据无data:image/png;base64,前缀）
     */
    public static String createQRImage(String content, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }

    /**
     * 压缩图片
     *
     * @param input       输入图片
     * @param output      输出图片（跟输入图片相同则会覆盖）
     * @param desFileSize 目标大小（单位：kb）
     */
    public static void compress(File input, File output, long desFileSize) throws IOException {
        byte[] bytes = FileUtils.readFileToByteArray(input);
        bytes = compressPictureForScale(bytes, desFileSize);
        if (output.exists()) {
            FileUtil.del(output);
        }
        FileUtils.writeByteArrayToFile(output, bytes);
    }

    /**
     * 根据指定大小压缩图片
     *
     * @param imageBytes  源图片字节数组
     * @param desFileSize 指定图片大小，单位 kb
     * @return 压缩质量后的图片字节数组
     */
    public static byte[] compressPictureForScale(byte[] imageBytes, long desFileSize) throws IOException {
        if (imageBytes == null || imageBytes.length <= 0 || imageBytes.length <= desFileSize * 1024) {
            return imageBytes;
        }
        long srcSize = imageBytes.length;
        double accuracy = getAccuracy(srcSize / 1024);
        while (imageBytes.length > desFileSize * 1024) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(imageBytes.length);
            Thumbnails.of(inputStream).scale(accuracy).outputQuality(accuracy).toOutputStream(outputStream);
            imageBytes = outputStream.toByteArray();
        }
        return imageBytes;
    }

    /**
     * 自动调节精度(经验数值)
     *
     * @param size 源图片大小，单位 kb
     * @return 图片压缩质量比
     */
    private static double getAccuracy(long size) {
        double accuracy;
        if (size < 512) {
            accuracy = 0.85;
        } else if (size < 1024) {
            accuracy = 0.8;
        } else if (size < 2048) {
            accuracy = 0.7;
        } else {
            accuracy = 0.6;
        }
        return accuracy;
    }

}
