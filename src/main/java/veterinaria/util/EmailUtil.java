package veterinaria.util;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtil {

    private static final String HOST = "smtp.gmail.com";
    private static final String PUERTO = "587";
    private static final String REMITENTE = "CONFIGURAR_EMAIL"; // Tu correo de la veterinaria
    private static final String PASSWORD = "CONFIGURAR_PASSWORD"; // Contraseña de aplicación de Gmail

    public static void enviarCodigo(String destinatario, String codigo) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", HOST);
        props.put("mail.smtp.port", PUERTO);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(REMITENTE, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(REMITENTE));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject("Código de recuperación de contraseña - Veterinaria");
            message.setText("Hola,\n\nTu código de verificación para restablecer la contraseña es: " + codigo
                    + "\n\nEste código expirará en 15 minutos.\nSi no solicitaste esto, ignorá este mensaje.");

            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el correo: " + e.getMessage(), e);
        }
    }
}
