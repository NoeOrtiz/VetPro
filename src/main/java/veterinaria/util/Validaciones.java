package veterinaria.util;

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validaciones {

    public void validarSoloNumeros(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (!Character.isDigit(c)) {
            evt.consume();
        }
    }

    public void validarSoloNumerosConDecimal(java.awt.event.KeyEvent evt, javax.swing.JTextField textField) {
        char c = evt.getKeyChar();
        if (!Character.isDigit(c) && c != '.' && c != ',') {
            evt.consume();
            return;
        }
        if ((c == '.' && textField.getText().contains(".")) || (c == ',' && textField.getText().contains(","))) {
            evt.consume();
            return;
        }
        if (c == '.' && textField.getText().contains(",")) {
            evt.consume();
        }
        if (c == ',' && textField.getText().contains(".")) {
            evt.consume();
        }
    }

    public void validarSoloLetras(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (!Character.isLetter(c) && c != ' ') {
            evt.consume();
        }
    }

    public void configurarValidacionCUIT(javax.swing.JTextField textField, javax.swing.JLabel labelValidacion) {
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                validarSoloNumeros(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                String cuitIngresado = textField.getText().replaceAll("-", "");
                StringBuilder cuitFormateado = new StringBuilder(cuitIngresado);

                if (cuitIngresado.length() > 2) {
                    cuitFormateado.insert(2, "-");
                }

                if (cuitIngresado.length() > 10) {
                    cuitFormateado.insert(11, "-");
                }

                textField.setText(cuitFormateado.toString());
                textField.setCaretPosition(textField.getText().length());

                if (textField.getText().length() == 13) {
                    if (validarCuitCuil(textField.getText())) {
                        labelValidacion.setText("Formato CUIT correcto");
                        labelValidacion.setForeground(Color.GREEN);
                    } else {
                        labelValidacion.setText("Formato CUIT incorrecto");
                        labelValidacion.setForeground(Color.RED);
                    }
                } else {
                    labelValidacion.setText("Formato incompleto");
                    labelValidacion.setForeground(Color.ORANGE);
                }
            }
        });
    }

    public boolean validarCuitCuil(String cuit) {
        if (cuit.length() != 13) {
            return false;
        }

        if (!cuit.substring(0, 2).matches("\\d{2}")) {
            return false;
        }

        if (cuit.charAt(2) != '-' || cuit.charAt(11) != '-') {
            return false;
        }

        if (!cuit.substring(3, 11).matches("\\d{8}")) {
            return false;
        }

        if (!cuit.substring(12).matches("\\d")) {
            return false;
        }
        return true;
    }

    public boolean validarCorreoValido(String email) {
        String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        if (email == null || email.isEmpty()) {
            return false;  // Correo vacío o nulo no es válido
        }
        Pattern pattern = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();  // Devuelve true si el correo coincide con el patrón
    }

    public static void validarFechaNoAnteriorALaActual(LocalDate fecha) throws Exception {
        LocalDate fechaActual = LocalDate.now();
        if (fecha.isBefore(fechaActual)) {
            throw new Exception("La fecha no puede ser anterior a la fecha actual.");
        }
    }

    public boolean validarComplejidadContrasena(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        String regex = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-])[A-Za-z\\d@$!%*?&._-]{8,}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }
}
