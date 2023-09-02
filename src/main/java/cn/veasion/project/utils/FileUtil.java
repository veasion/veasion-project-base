package cn.veasion.project.utils;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.util.IOUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * FileUtil
 *
 * @author luozhuowei
 */
public class FileUtil {

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    /**
     * 系统临时目录
     * <br>
     * windows 包含路径分割符，但Linux 不包含,
     * 在windows \\==\ 前提下，
     * 为安全起见 同意拼装 路径分割符，
     * <pre>
     *       java.io.tmpdir
     *       windows : C:\Users/xxx\AppData\Local\Temp\
     *       linux: /tmp
     * </pre>
     */
    public static final String SYS_TEM_DIR;

    static {
        String temp = System.getProperty("java.io.tmpdir");
        SYS_TEM_DIR = temp.endsWith("/") || temp.endsWith("\\") ? temp : (temp + File.separator);
    }

    /**
     * MultipartFile转File
     */
    public static File toFile(MultipartFile multipartFile) {
        // 获取文件名
        String fileName = multipartFile.getOriginalFilename();
        // 获取文件后缀
        String suffix = getExtensionName(fileName);
        if (StringUtils.isNotEmpty(suffix)) {
            suffix = "." + suffix;
        }
        File file = null;
        try {
            // 用uuid作为文件名，防止生成的临时文件重复
            file = new File(SYS_TEM_DIR + UUID.simpleUUID() + suffix);
            multipartFile.transferTo(file);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return file;
    }

    /**
     * 临时文件
     */
    public static File tempFile(String suffix) {
        if (!suffix.startsWith(".")) {
            suffix = "." + suffix;
        }
        return new File(FileUtil.SYS_TEM_DIR + UUID.simpleUUID() + suffix);
    }

    /**
     * 新文件
     */
    public static File newFile(File source, BiFunction<String, String, String> tempName) {
        if (source == null) {
            return new File(FileUtil.SYS_TEM_DIR + UUID.simpleUUID() + tempName.apply("", ""));
        } else {
            String parent = source.getParentFile().getAbsolutePath();
            String name = source.getName();
            String suffix = "";
            int idx = name.lastIndexOf(".");
            if (idx > -1) {
                suffix = name.substring(idx);
                name = name.substring(0, idx);
            }
            return new File(parent + File.separator + tempName.apply(name, suffix));
        }
    }

    public static File mkParentDirs(File file) {
        File parentFile = file.getParentFile();
        if (null != parentFile && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        return parentFile;
    }

    public static File mkdir(File dir) {
        if (dir == null) {
            return null;
        } else {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return dir;
        }
    }

    public static void move(File src, File dest, boolean isOverride) {
        if (!src.exists()) {
            throw new IORuntimeException("File not found: " + src);
        } else if (src.isDirectory() && dest.isFile()) {
            throw new IORuntimeException("Can not move directory [" + src + "] to file [" + dest + "]");
        } else {
            if (isOverride && dest.isFile()) {
                dest.delete();
            }
            if (src.isFile() && dest.isDirectory()) {
                dest = new File(dest, src.getName());
            }
            if (!src.renameTo(dest)) {
                try {
                    copy(src, dest, isOverride);
                } catch (Exception var4) {
                    throw new IORuntimeException("Move [" + src + "] to [" + dest + "] failed!", var4);
                }
                del(src);
            }
        }
    }

    public static File copy(File src, File dest, boolean isOverride) {
        if (src == null) {
            throw new IORuntimeException("Source File is null !");
        }
        if (!src.exists()) {
            throw new IORuntimeException("File not exist: " + src);
        } else {
            if (dest == null) {
                throw new IORuntimeException("Destination File or directiory is null !");
            }
            if (Objects.equals(src, dest)) {
                throw new IORuntimeException("Files '" + src + "' and '" + dest + "' are equal");
            } else {
                if (src.isDirectory()) {
                    if (dest.exists() && !dest.isDirectory()) {
                        throw new IORuntimeException("Src is a directory but dest is a file!");
                    }
                    if (isSub(src, dest)) {
                        throw new IORuntimeException("Dest is a sub directory of src !");
                    }
                    File subDest = mkdir(checkSlip(dest, new File(dest, src.getName())));
                    internalCopyDirContent(src, subDest, isOverride);
                } else {
                    internalCopyFile(src, dest, isOverride);
                }
                return dest;
            }
        }
    }

    private static void internalCopyDirContent(File src, File dest, boolean isOverride) {
        if (!dest.exists()) {
            dest.mkdirs();
        } else if (!dest.isDirectory()) {
            throw new IORuntimeException("Src [" + src.getPath() + "] is a directory but dest [" + dest.getPath() + "] is a file!");
        }
        String[] files = src.list();
        if (files != null) {
            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                if (srcFile.isDirectory()) {
                    internalCopyDirContent(srcFile, destFile, isOverride);
                } else {
                    internalCopyFile(srcFile, destFile, isOverride);
                }
            }
        }
    }

    private static void internalCopyFile(File src, File dest, boolean isOverride) {
        if (dest.exists()) {
            if (dest.isDirectory()) {
                dest = new File(dest, src.getName());
            }

            if (dest.exists() && !isOverride) {
                return;
            }
        } else {
            dest.getParentFile().mkdirs();
        }

        ArrayList<CopyOption> optionList = new ArrayList<>(2);
        if (isOverride) {
            optionList.add(StandardCopyOption.REPLACE_EXISTING);
        }
        optionList.add(StandardCopyOption.COPY_ATTRIBUTES);
        try {
            Files.copy(src.toPath(), dest.toPath(), optionList.toArray(new CopyOption[0]));
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static File checkSlip(File parentFile, File file) throws IllegalArgumentException {
        if (null != parentFile && null != file) {
            String parentCanonicalPath;
            String canonicalPath;
            try {
                parentCanonicalPath = parentFile.getCanonicalPath();
                canonicalPath = file.getCanonicalPath();
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
            if (!canonicalPath.startsWith(parentCanonicalPath)) {
                throw new IllegalArgumentException("New file is outside of the parent dir: " + file.getName());
            }
        }
        return file;
    }

    public static boolean isSub(File parent, File sub) {
        return sub.toPath().startsWith(parent.toPath());
    }

    /**
     * 获取文件扩展名，不带 .
     */
    public static String getExtensionName(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot > -1 && dot < (filename.length() - 1)) {
            return filename.substring(dot + 1);
        }
        return "";
    }

    /**
     * Java文件操作 获取不带扩展名的文件名
     */
    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if (dot > -1) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    public static void writeUtf8String(String content, File file) {
        writeString(content, file, StandardCharsets.UTF_8, false);
    }

    public static void appendUtf8String(String content, File file) {
        appendString(content, file, StandardCharsets.UTF_8);
    }

    public static void appendString(String content, File file, Charset charset) {
        writeString(content, file, charset, true);
    }

    public static void writeString(String content, File file, Charset charset, boolean append) {
        if (!file.exists()) {
            mkParentDirs(file);
            try {
                file.createNewFile();
            } catch (Exception e) {
                throw new IORuntimeException(e);
            }
        }
        try (OutputStream outputStream = new FileOutputStream(file, append)) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, charset))) {
                writer.write(content);
                writer.flush();
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static List<String> readLines(File file) {
        return readLines(file, StandardCharsets.UTF_8);
    }

    public static List<String> readLines(File file, Charset charset) {
        try {
            return Files.readAllLines(file.toPath(), charset != null ? charset : StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IORuntimeException(e);
        }
    }

    public static String readUtf8String(File file) {
        return readString(file, StandardCharsets.UTF_8);
    }

    public static String readString(File file, Charset charset) {
        try {
            return FileUtils.readFileToString(file, charset);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
     * inputStream 转 File
     */
    static File inputStreamToFile(InputStream ins, String name) {
        File file = new File(SYS_TEM_DIR + name);
        if (file.exists()) {
            return file;
        }
        OutputStream os = null;
        try {
            os = Files.newOutputStream(file.toPath());
            int bytesRead;
            int len = 8192;
            byte[] buffer = new byte[len];
            while ((bytesRead = ins.read(buffer, 0, len)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (ins != null) {
                    ins.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    /**
     * 将文件名解析成文件的上传路径
     */
    public static File upload(MultipartFile file, String filePath) {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmssS");
        String name = getFileNameNoEx(file.getOriginalFilename());
        String suffix = getExtensionName(file.getOriginalFilename());
        if (StringUtils.isNotEmpty(suffix)) {
            suffix = "." + suffix;
        }
        String nowStr = "-" + format.format(date);
        try {
            String fileName = name + nowStr + suffix;
            String path = filePath + fileName;
            File dest = new File(path).getCanonicalFile();
            // 检测是否存在目录
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            // 文件写入
            file.transferTo(dest);
            return dest;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static void checkSize(long maxSize, long size) {
        // 10M
        int len = 10 * 1024 * 1024;
        if (size > (maxSize * len)) {
            throw new IORuntimeException("文件超出规定大小");
        }
    }

    /**
     * 判断两个文件是否相同
     */
    public static boolean check(File file1, File file2) {
        String img1Md5 = getMd5(file1);
        String img2Md5 = getMd5(file2);
        if (img1Md5 != null) {
            return img1Md5.equals(img2Md5);
        }
        return false;
    }

    public static boolean del(File file) {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                boolean isOk = clean(file);
                if (!isOk) {
                    return false;
                }
            }
            return file.delete();
        } else {
            return true;
        }
    }

    public static boolean clean(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (File childFile : files) {
                    boolean isOk = del(childFile);
                    if (!isOk) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 判断两个文件是否相同
     */
    public static boolean check(String file1Md5, String file2Md5) {
        return file1Md5.equals(file2Md5);
    }

    private static byte[] getByte(File file) {
        // 得到文件长度
        byte[] b = new byte[(int) file.length()];
        try (InputStream in = Files.newInputStream(file.toPath())) {
            try {
                in.read(b);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return b;
    }

    public static String getMd5(byte[] bytes) {
        // 16进制字符
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(bytes);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            // 移位 输出字符串
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 下载文件
     */
    public static void downloadFile(HttpServletResponse response, File file, boolean deleteOnExit) {
        response.setContentType("application/octet-stream");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
            IOUtils.copy(fis, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                    if (deleteOnExit) {
                        file.deleteOnExit();
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static String getMd5(File file) {
        return getMd5(getByte(file));
    }

}
