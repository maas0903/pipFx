/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pipFx;

/**
 *
 * @author Marius
 */
import com.sun.mail.smtp.SMTPTransport;
import java.net.URL;
import java.net.URLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class pipFx {

    static Integer Interval;
    static String EmailAddressTo;
    static String EmailAddressFrom;
    static String IpFile;
    static String EmailPasswordFrom;
    static String ProxyToUse;
    static String ProxyPortToUse;
    static String TestMode;
    static boolean bTestMode;
    static String RemotePublicIpNameForEmail;
    static String EmailAddressCC;

    public static void Log(String message) {
        System.out.println(message);
    }

    public static boolean EMailSender(String host, String port, final String from, final String pass, String to, String cc, String sub, String mess) {
        Properties props = System.getProperties();
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.setProperty("mail.smtp.host", host);
        props.setProperty("mail.smtp.port", port);
        Authenticator auth;
        auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, pass);
            }
        };
        try {
            Session session = Session.getInstance(props, auth);
            //session.setDebug(verbose);
            Transport tr;
            tr = session.getTransport("smtp");
            if (!(tr instanceof SMTPTransport)) {
                Log("This is NOT an SMTPTransport:[" + tr.getClass().getName() + "]");
            }
            Message message;
            message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
            message.setSubject(sub);
            message.setText(mess);
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            Log("Exception in EMailSender:" + e.getMessage());
            return false;
        }
    }

    public static void sendMail(String Ip) {
        GetProperties();
        String host = "smtp.gmail.com";
        String port = "587";
        String from = EmailAddressFrom;
        String password = EmailPasswordFrom;
        String to = EmailAddressTo;
        String cc = EmailAddressCC;
        String subject = RemotePublicIpNameForEmail + " Public IP Change";
        String message = Ip;

        for (int i = 1; i < 11; i++) {

            try {
                if (EMailSender(host, port, from, password, to, cc, subject, message)) {
                    Log("Try number " + i + " Email sent.");
                    break;
                } else {
                    Log("Try number " + i + " Email not sent. Sleeping for 5s");
                    Thread.sleep(5000);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(pipFx.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private static String GetPublicIp() throws Exception {
        URL whatismyip = new URL("http://checkip.amazonaws.com/");
        URLConnection connection;
        if (ProxyToUse.isEmpty()) {
            connection = whatismyip.openConnection();
        } else {
            Log("  proxy=" + ProxyToUse);
            Log("Using proxy");
            Log("  port=" + ProxyPortToUse);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ProxyToUse, Integer.parseInt(ProxyPortToUse)));
            connection = whatismyip.openConnection(proxy);
        }
        connection.addRequestProperty("Protocol", "Http/1.1");
        connection.addRequestProperty("Connection", "keep-alive");
        connection.addRequestProperty("Keep-Alive", "1000");
        connection.addRequestProperty("User-Agent", "Web-Agent");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String ip = in.readLine();
        return ip;
    }

    private static String readFromInputStream(InputStream inputStream) throws Exception {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line);
            }
        }
        return resultStringBuilder.toString();
    }

    private static String GetDummyPublicIp() throws Exception {
        return "147.135.68.68";
    }

    private static String GetFileIp(String Ip) throws Exception {
        String data = "";
        File file = new File(IpFile);
        FileInputStream fis = null;

        try {
            if (file.exists()) {
                fis = new FileInputStream(file);
                int content;
                while ((content = fis.read()) != -1) {
                    data += (char) content;
                }
            } else {
                SetFileIp(Ip);
                data = Ip;
                Log("  File created");
                sendMail(Ip);
            }
        } catch (Exception e) {
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
            }
        }
        return data;
    }

    private static void SetFileIp(String Ip) throws Exception {
        FileOutputStream fos = null;
        File file;

        try {
            file = new File(IpFile);
            fos = new FileOutputStream(file);

            if (!file.exists()) {
                file.createNewFile();
            }

            byte[] contentInBytes = Ip.getBytes();

            fos.write(contentInBytes);
            fos.flush();
            fos.close();
        } catch (IOException e) {
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
            }
        }
    }

    private static void SetProperties() {
        Properties prop = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream("config.properties");
            prop.setProperty("Interval", "5000");
            prop.setProperty("EmailAddressTo", "anemailaddressTo@email.com");
            prop.setProperty("EmailAddressFrom", "anemailaddressFrom@email.com");
            prop.setProperty("EmailAddressCC", "anemailaddressCC@email.com");
            prop.setProperty("EmailPasswordFrom", "emailPassword");
            prop.setProperty("IpFile", "fileIP.txt");
            prop.setProperty("ProxyToUse", "");
            prop.setProperty("ProxyPortToUse", "8080");
            prop.setProperty("TestMode", "false");
            prop.setProperty("RemotePublicIpNameForEmail", "RemoteIP");
            prop.store(output, null);

        } catch (IOException io) {
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static String LoadProperty(Properties prop, String property, String defaultvalue) {
        String PropertyValue = prop.getProperty(property);
        if (PropertyValue != null) {
            return PropertyValue;
        } else {
            return defaultvalue;
        }
    }

    private static void GetProperties() {
        Properties prop = new Properties();
        InputStream input = null;

        File file = new File("config.properties");

        if (!file.exists()) {
            SetProperties();
            Log("Please configure the properties in the 'config.properties' file.");
            System.exit(0);
        } else {
            try {
                input = new FileInputStream(file);
                prop.load(input);

                Interval = Integer.parseInt(LoadProperty(prop, "Interval", "10"));
                EmailAddressTo = LoadProperty(prop, "EmailAddressTo", "");
                EmailAddressCC = LoadProperty(prop, "EmailAddressCC", "");
                EmailAddressFrom = LoadProperty(prop, "EmailAddressFrom", "");
                EmailPasswordFrom = LoadProperty(prop, "EmailPasswordFrom", "");
                IpFile = LoadProperty(prop, "IpFile", "fileIP.txt");
                ProxyToUse = LoadProperty(prop, "ProxyToUse", "");
                ProxyPortToUse = LoadProperty(prop, "ProxyPortToUse", "");
                TestMode = LoadProperty(prop, "TestMode", "false").toLowerCase();
                RemotePublicIpNameForEmail = LoadProperty(prop, "RemotePublicIpNameForEmail", "domain");
                bTestMode = !TestMode.equals("false");

            } catch (IOException ex) {
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    private static void CompareIp(Boolean bSendAnyway) {
        try {
            String publicIpAddress;
            if (bTestMode) {
                publicIpAddress = GetDummyPublicIp();
            } else {
                publicIpAddress = GetPublicIp();
            }
            String fileIpAddress = GetFileIp(publicIpAddress);
            if (!fileIpAddress.equals(publicIpAddress)) {
                Log("Not Same!!");
                Log("  File Ip Address  =" + fileIpAddress);
                Log("  Public Ip Address=" + publicIpAddress);
                sendMail(publicIpAddress);
                SetFileIp(publicIpAddress);
            }
        } catch (Exception e) {
        }
    }

    private static void FileCompare() {
        File file = new File("setpole.txt");
        if (file.exists()) {
            CompareIp(true);
            file.delete();
        }
    }

    public static void main(String[] args) throws Exception {
        GetProperties();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                CompareIp(false);
            }
        }, 0, Interval);

        Timer filetimer = new Timer();
        filetimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                FileCompare();
            }
        }, 0, 10000);
    }
}
