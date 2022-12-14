package cn.veasion.project.utils;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;
import java.util.Date;
import java.util.Properties;

/**
 * MailUtils
 *
 * @author luozhuowei
 * @date 2022/2/22
 */
public class MailUtils {

    private final static int TIMEOUT_MS = 20000;

    /**
     * 发送邮件
     */
    public static void send(MailVO mailVO) throws Exception {
        boolean auth = StringUtils.isNotEmpty(mailVO.getPassword());
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", mailVO.getHost());
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.timeout", String.valueOf(TIMEOUT_MS));

        if (mailVO.isStarttlsEnable()) {
            props.put("mail.smtp.starttls.enable", "true");
            mailVO.setSslEnable(true);
        }

        if (mailVO.isSslEnable()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("smtp.socketFactory.port", mailVO.getSslPort() != null ? mailVO.getSslPort() : 465);
        }

        Session session;
        if (auth) {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailVO.getUser(), mailVO.getPassword());
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        MimeMessage msg = new MimeMessage(session);
        if (StringUtils.isNotEmpty(mailVO.getNickName())) {
            msg.setFrom(mailVO.getNickName() + "<" + mailVO.getUser() + ">");
        } else {
            msg.setFrom(mailVO.getUser());
        }

        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(mailVO.getRecipients()));
        if (StringUtils.isNotEmpty(mailVO.getCc())) {
            msg.setRecipients(Message.RecipientType.CC, mailVO.getCc());
        }

        // 标题
        msg.setSubject(mailVO.getSubject());

        // 邮件正文
        Multipart multipart = new MimeMultipart();
        BodyPart contentPart = new MimeBodyPart();
        contentPart.setContent(mailVO.getContent(), "text/html;charset=UTF-8");
        multipart.addBodyPart(contentPart);
        // 添加附件
        if (mailVO.getAttachment() != null) {
            BodyPart attachmentBodyPart = new MimeBodyPart();
            DataSource source = new ByteArrayDataSource(mailVO.getAttachment(), "application/octet-stream");
            attachmentBodyPart.setDataHandler(new DataHandler(source));
            // MimeUtility.encodeWord可以避免文件名乱码
            attachmentBodyPart.setFileName(MimeUtility.encodeWord(mailVO.getAttachmentName()));
            multipart.addBodyPart(attachmentBodyPart);
        }
        msg.setContent(multipart);

        if (mailVO.isSaveChanges()) {
            // 保存邮件
            msg.saveChanges();
        }
        msg.setSentDate(new Date());
        Transport.send(msg, msg.getAllRecipients());
    }

    public static class MailVO {
        /**
         * 邮件服务器主机名
         */
        private String host;
        /**
         * 用户昵称
         */
        private String nickName;

        /**
         * 用户名
         */
        private String user;
        /**
         * 密码
         */
        private String password;

        private boolean sslEnable;
        private boolean starttlsEnable;
        private Integer sslPort = 465;

        /**
         * 收件人
         */
        private String recipients;
        /**
         * 抄送人
         */
        private String cc;
        /**
         * 主题
         */
        private String subject;
        /**
         * 内容
         */
        private String content;
        /**
         * 附件
         */
        private byte[] attachment;
        /**
         * 附件文件名
         */
        private String attachmentName;

        private boolean saveChanges;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isSslEnable() {
            return sslEnable;
        }

        public void setSslEnable(boolean sslEnable) {
            this.sslEnable = sslEnable;
        }

        public boolean isStarttlsEnable() {
            return starttlsEnable;
        }

        public void setStarttlsEnable(boolean starttlsEnable) {
            this.starttlsEnable = starttlsEnable;
        }

        public Integer getSslPort() {
            return sslPort;
        }

        public void setSslPort(Integer sslPort) {
            this.sslPort = sslPort;
        }

        public String getRecipients() {
            return recipients;
        }

        public void setRecipients(String recipients) {
            this.recipients = recipients;
        }

        public String getCc() {
            return cc;
        }

        public void setCc(String cc) {
            this.cc = cc;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public byte[] getAttachment() {
            return attachment;
        }

        public void setAttachment(byte[] attachment) {
            this.attachment = attachment;
        }

        public String getAttachmentName() {
            return attachmentName;
        }

        public void setAttachmentName(String attachmentName) {
            this.attachmentName = attachmentName;
        }

        public boolean isSaveChanges() {
            return saveChanges;
        }

        public void setSaveChanges(boolean saveChanges) {
            this.saveChanges = saveChanges;
        }
    }

}
