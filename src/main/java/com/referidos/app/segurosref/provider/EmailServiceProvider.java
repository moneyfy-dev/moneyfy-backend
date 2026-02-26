package com.referidos.app.segurosref.provider;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

// import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.models.QuoterAddressModel;
import com.referidos.app.segurosref.models.QuoterCarModel;
import com.referidos.app.segurosref.models.QuoterModel;
import com.referidos.app.segurosref.models.QuoterOwnerModel;
import com.referidos.app.segurosref.models.QuoterPlanModel;
import com.referidos.app.segurosref.models.QuoterPurchaserModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;

import jakarta.mail.internet.MimeMessage;

@Component
public class EmailServiceProvider {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value(value = "${mail.sender.user}")
    private String sender;

    @Value(value = "${mail.sender.user-alias}")
    private String senderAlias;

    @Value(value = "${mail.support.money.fy}")
    private String supportMoneyFy;

    @Value(value = "${mail.subject.auth-code}")
    private String subjectAuthCode;

    @Value(value = "${mail.template.auth-code}")
    private String templateAuthCode;

    @Value(value = "${mail.subject.security-code}")
    private String subjectSecurityCode;

    @Value(value = "${mail.template.security-code}")
    private String templateSecurityCode;

    @Value(value = "${mail.subject.user-account-activated}")
    private String subjectUserAccountActivated;

    @Value(value = "${mail.template.user-account-activated}")
    private String templateUserAccountActivated;

    @Value(value = "${mail.subject.user-account-disabled}")
    private String subjectUserAccountDisabled;

    @Value(value = "${mail.template.user-account-disabled}")
    private String templateUserAccountDisabled;

    @Value(value = "${mail.subject.nova-user-referred}")
    private String subjectNovaUserReferred;

    @Value(value = "${mail.template.nova-user-referred}")
    private String templateNovaUserReferred;

    @Value(value = "${mail.subject.quoter-details-purchaser}")
    private String subjectQuoterDetailsPurchaser;

    @Value(value = "${mail.template.quoter-details-purchaser}")
    private String templateQuoterDetailsPurchaser;

    @Value(value = "${mail.subject.vehicle-details-user}")
    private String subjectVehicleDetailsUser;

    @Value(value = "${mail.template.vehicle-details-user}")
    private String templateVehicleDetailsUser;
    
    @Value(value = "${mail.subject.notify-account-not-found}")
    private String subjectNotifyAccountNotFound;

    @Value(value = "${mail.template.notify-account-not-found}")
    private String templateNotifyAccountNotFound;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void sendAuthCodeToRegisterUser(String[] toUsers, String code) {
        try {
            Map<String, Object> templateData = this.buildEmailTemplateData(code, 1);
            this.sendEmail(toUsers, subjectAuthCode, templateData, templateAuthCode);
        } catch(Exception e) {
            String message = this.buildEmailMessage(code, 1);
            this.testEmail(toUsers, subjectAuthCode, message);
        }
    }

    public void sendAuthCodeToChangeDevice(String[] toUsers, String code) {
        try {
            Map<String, Object> templateData = this.buildEmailTemplateData(code, 2);
            this.sendEmail(toUsers, subjectAuthCode, templateData, templateAuthCode);
        } catch(Exception e) {
            String message = this.buildEmailMessage(code, 2);
            this.testEmail(toUsers, subjectAuthCode, message);
        }
    }

    public void sendAuthCodeToRestorePassword(String[] toUsers, String code) {
        try {
            Map<String, Object> templateData = this.buildEmailTemplateData(code, 3);
            this.sendEmail(toUsers, subjectAuthCode, templateData, templateAuthCode);
        } catch(Exception e) {
            String message = this.buildEmailMessage(code, 3);
            this.testEmail(toUsers, subjectAuthCode, message);
        }
    }

    public void userAccountActivated(String userEmail, String device, String deviceIp) {
        String activateDate = LocalDateTime.now().format(this.dateFormatter);
        String[] toUsers = {userEmail};
        try {
            Map<String, Object> templateData  = Map.of("userEmail", userEmail, "device", device, "deviceIp", deviceIp, "activateDate", activateDate, "support", supportMoneyFy);
            this.sendEmail(toUsers, subjectUserAccountActivated, templateData, templateUserAccountActivated);
        } catch(Exception e) {
            StringBuilder sb = new StringBuilder("¡Nos alegra saber que has regresado!\n\n");
            sb.append("Se ha vuelto activar tu cuenta de usuario en nuestra aplicación, debido a que, no ha transcurrido el tiempo estipulado de 30 días hábiles para ser completamente eliminado. Los datos de inicio de sesión son:\n\n");
            sb.append("Usuario: ").append(userEmail).append("\n");
            sb.append("Dispositivo: ").append(device).append("\n");
            sb.append("Ip del dispositivo: ").append(deviceIp).append("\n");
            sb.append("Horario de inicio de sesión: ").append(activateDate).append("\n\n");
            sb.append("Si necesitas ayuda, puedes comunicarte con: ").append(supportMoneyFy).append("\n");
            sb.append("Este es un mensaje automático, por favor no lo responda.");
            this.testEmail(toUsers, subjectUserAccountActivated, sb.toString());
        }
    }

    public void userAccountDisabled(String userEmail, LocalDateTime disableDateTime) {
        String disableDate = disableDateTime.format(this.dateFormatter);
        String deleteDate = disableDateTime.toLocalDate().plusDays(31).atStartOfDay().format(this.dateFormatter);
        String[] toUsers = {userEmail};
        try {
            Map<String, Object> templateData = Map.of("userEmail", userEmail, "disableDate", disableDate, "deleteDate", deleteDate, "support", supportMoneyFy);
            this.sendEmail(toUsers, subjectUserAccountDisabled, templateData, templateUserAccountDisabled);
        } catch(Exception e) {
            StringBuilder sb = new StringBuilder("Tu cuenta de usuario se ha desactivado exitosamente\n\n");
            sb.append("Se ha programado la eliminación de tu cuenta, luego de transcurrido 30 días de inactividad. Si deseas volver ha activarla, solo necesitas registrarte con tus credenciales, antes de completarse el tiempo estipulado. ¡Te esperaremos por si cambias de opinión!\n\n");
            sb.append("Información de desactivación de la cuenta:\n");
            sb.append("Usuario: ").append(userEmail).append("\n");
            sb.append("Fecha de desactivación de cuenta: ").append(disableDate).append("\n");
            sb.append("Fecha para la eliminación programada: ").append(deleteDate).append("\n\n");
            sb.append("Si necesitas ayuda, puedes comunicarte con: ").append(supportMoneyFy).append("\n");
            sb.append("Este es un mensaje automático, por favor no lo responda.");
            this.testEmail(toUsers, subjectUserAccountDisabled, sb.toString());
        }
    }

    public void novaUserRegisteredByCodeToRefer(String userEmailA, String userACodeToRefer, String fullNameReferredUser) {
        String[] toUsers = {userEmailA};
        try {
            Map<String, Object> templateData = Map.of("userACodeToRefer", userACodeToRefer, "fullNameReferredUser", fullNameReferredUser);
            this.sendEmail(toUsers, subjectNovaUserReferred, templateData, templateNovaUserReferred);
        } catch(Exception e) {
            StringBuilder sb = new StringBuilder("¡Se ha registrado un nuevo usuario con tu código de referidos ").append(userACodeToRefer).append("!\n\n");
            sb.append("El usuario ").append(fullNameReferredUser).append(", acaba de registrarse con tu código de referidos, sigue así y refiere a muchos más, recuerda que entre más refieres más lucas!\n\n");
            sb.append("Este es un mensaje automático, por favor no lo responda.");
            this.testEmail(toUsers, subjectNovaUserReferred, sb.toString());
        }
    }

    public void sendQuoteDetails(UserModel userDB, QuoterModel quoterDB) {
        // Data relacionada al usuario de la aplicación
        UserDataModel userDataDB = userDB.getPersonalData();
        String userFullName = userDataDB.getName() + " " + userDataDB.getSurname();
        String[] toUsers1 = {userDataDB.getEmail()};
        
        // Data relacionada a la cotización
        String quoterId = quoterDB.getQuoterId();
        
        // Los asuntos van con el id de la cotización
        String subjectPersonalized1 = subjectQuoterDetailsPurchaser + quoterId;
        String subjectPersonalized2 = subjectVehicleDetailsUser + quoterId;

        
        QuoterOwnerModel quoterOwner = quoterDB.getQuoterOwnerData();
        String ownerFullName = quoterOwner.getName() + " " + quoterOwner.getPaternalSurname() + " " + quoterOwner.getMaternalSurname();
        
        QuoterCarModel quoterCar = quoterDB.getQuoterCarData();
        
        QuoterPurchaserModel quoterPurchaser = quoterDB.getQuoterPurchaserData();
        String[] toUsers2 = {quoterPurchaser.getEmail()};
        String purchaserFullName = quoterPurchaser.getName() + " " + quoterPurchaser.getPaternalSurname() + " " + quoterPurchaser.getMaternalSurname();
        
        QuoterPlanModel quoterPlan = quoterDB.getQuoterPlanData();
        
        QuoterAddressModel quoterAddress = quoterDB.getQuoterAddressData();
        String addressDetail = quoterAddress.getStreet() + " " + quoterAddress.getStreetNumber();
        String departmentDetail = !DataHelper.isNull(quoterAddress.getDepartment()) ? quoterAddress.getDepartment() : "Sin especificar";

        try {
            // DESARROLLAMOS EL EMAIL PARA EL COMPRADOR, INCLUYENDO LA DATA DE LA PLANTILLA
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("quoterId", quoterId);
            templateData.put("ownerFullName", ownerFullName);
            templateData.put("purchaserFullName", purchaserFullName);
            templateData.put("addressDetail", addressDetail);
            templateData.put("departmentDetail", departmentDetail);
            templateData.put("quoterCar", quoterCar);
            templateData.put("quoterPurchaser", quoterPurchaser);
            templateData.put("quoterPlan", quoterPlan);
            // Dirigimos al comprador de la cotización
            this.sendEmail(toUsers2, subjectPersonalized1, templateData, templateQuoterDetailsPurchaser);
        } catch (Exception e) {
            LOGGER_MESSAGES.info("Ha ocurrido un proceso inesperado al enviar el email de detalle de la cotización para el comprador: " + e.getMessage());
            // DESARROLLAMOS EL EMAIL PARA EL COMPRADOR, INCLUYENDO LA DATA DE LA PLANTILLA
            StringBuilder sb = new StringBuilder("Hola " + purchaserFullName + ",\n\n");
            sb.append("Gracias por solicitar tu cotización a través de nuestra aplicación. Queremos informarte que la solicitud está siendo procesada por la aseguradora. En caso de requerir información adicional, nos pondremos en contacto contigo.\n\n");
            sb.append("A continuación, te compartimos los detalles de la cotización N°").append(quoterId).append("\n\n");
            sb.append("Vehículo Cotizado\n");
            sb.append("Propietario: ").append(ownerFullName).append("\nPantente: ").append(quoterCar.getPpu()).append("\nMarca: ").append(quoterCar.getBrand()).append("\n");
            sb.append("Modelo: ").append(quoterCar.getModel()).append("\nAño: ").append(quoterCar.getYear()).append("\n\n");
            sb.append("Plan Seleccionado\n");
            sb.append("Aseguradora: ").append(quoterPlan.getInsurer()).append("\nNombre del plan: ").append(quoterPlan.getPlanName()).append("\n");
            sb.append("Valor total: ").append(quoterPlan.getGrossPriceUF()).append(" UF\n").append("Valor mensual: ").append(quoterPlan.getMonthlyPriceUF()).append(" UF\n");
            sb.append("Cuotas: ").append(quoterPlan.getTotalMonths()).append("\n").append("Deducible: ").append(quoterPlan.getDeductible()).append(" UF\n\n");
            sb.append("Comprador\n");
            sb.append("Rut: ").append(quoterPurchaser.getPersonalId()).append("\n").append("Nombre: ").append(purchaserFullName).append("\n\n");
            sb.append("Dirección de Inspección").append("\n");
            sb.append("Calle: ").append(addressDetail).append("\n").append("Departamento (opcional): ").append(departmentDetail).append("\n\n");
            sb.append("Si necesitas ayuda, puedes comunicarte con: soporte@moneyfy.cl\nEste es un mensaje automático, por favor no lo responda.");
            this.testEmail(toUsers2, subjectPersonalized1, sb.toString());
        }

        try {
            // DESARROLLAMOS EL EMAIL PARA EL USUARIO, INCLUYENDO LA DATA DE LA PLANTILLA
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("quoterId", quoterId);
            templateData.put("userFullName", userFullName);
            templateData.put("quoterCar", quoterCar);
            // Dirigimos al usuario de la cotización
            this.sendEmail(toUsers1, subjectPersonalized2, templateData, templateVehicleDetailsUser);
        } catch(Exception e) {
            LOGGER_MESSAGES.info("Ha ocurrido un proceso inesperado al enviar el email para la cotización, vinculada al usuario: " + e.getMessage());
            StringBuilder sb = new StringBuilder("Hola " + userFullName + ",\n\n");
            sb.append("Queremos informarte que se ha iniciado el proceso de una nueva cotización con el N°").append(quoterId).append(" vinculada a tu cuenta.\n\n");
            sb.append("Vehículo Cotizado\n");
            sb.append("Pantente: ").append(quoterCar.getPpu()).append("\nMarca: ").append(quoterCar.getBrand()).append("\n");
            sb.append("Modelo: ").append(quoterCar.getModel()).append("\nAño: ").append(quoterCar.getYear()).append("\n\n");
            sb.append("Te mantendremos al tanto en caso de cualquier novedad. ¡Gracias por confiar en nosotros!\n\n");
            sb.append("Si necesitas ayuda, puedes comunicarte con: soporte@moneyfy.cl\nEste es un mensaje automático, por favor no lo responda.");
            this.testEmail(toUsers1, subjectPersonalized2, sb.toString());
        }

    }

    public void notifyAccountNotFound(List<String> toUsers) {
        String[] sendToUsers = new String[toUsers.size()];
        int i=0;
        for(String toUser : toUsers) {
            sendToUsers[i] = toUser;
            i += 1;
        }
        try {
            this.sendEmail(sendToUsers, subjectNotifyAccountNotFound, null, templateNotifyAccountNotFound);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("¡Se están procesando tus comisiones y no hemos podido identificar tu cuenta para el recibo!\n\n");
            sb.append("Verifica tu cuenta bancaria predeterminada para el recibo de comisiones en la aplicación.\n");
            sb.append("Si tu cuenta no puede ser identificada, tus pagos de comisiones serán programadas para el próximo mes.");
            this.testEmail(sendToUsers, subjectNotifyAccountNotFound, sb.toString());
        }
    }

    private void sendEmail(String[] toUsers, String subject, Map<String, Object> templateData, String htmlTemplate) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(toUsers);
        helper.setSubject(subject);
        helper.setFrom(sender, senderAlias);

        // Procesar la plantilla y obtener el contenido HTML
        Context context = new Context();
        context.setVariables(templateData);
        String htmlContent = templateEngine.process(htmlTemplate, context);

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    // Información dinámica que se agrega a la plantilla
    private Map<String, Object> buildEmailTemplateData(String code, int option) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("code", code);
        switch(option) {
            case 1 -> {
                templateData.put("info1", "Te ayudará a finalizar el proceso de registro, mantenlo seguro y no lo compartas con nadie.");
                break;
            }
            case 2 -> {
                templateData.put("info1", "Te ayudará a actualizar el dispositivo relacionado a tu cuenta, mantenlo seguro y no lo compartas con nadie.");
                templateData.put("info2", "Si necesitas ayuda, puedes comunicarte con: " + supportMoneyFy);
                break;
            }
            case 3 -> {
                templateData.put("info1", "Te ayudará a restablecer la contraseña de tu cuenta de usuario, mantenlo seguro y no lo compartas con nadie.");
                break;
            }
            default -> {
                templateData.put("info1", "Te ayudará a finalizar procesos que necesitan confirmación, mantenlo seguro y no lo compartas con nadie.");
                break;
            }
        }
        return templateData;
    }

    private void testEmail(String[] toUsers, String subject, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            // Establecer el remitente con nombre personalizado
            helper.setFrom(sender, senderAlias);

            helper.setTo(toUsers);
            helper.setSubject(subject);
            helper.setText(message, false); // El segundo parámetro indica si el contenido es HTML (false para texto plano)

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            LOGGER_MESSAGES.info("No es posible enviar el gmail en texto plano: " + e.getMessage());
        }
    }

    private String buildEmailMessage(String code, int option) {
        StringBuilder sb = new StringBuilder();
        sb.append("Su código de confirmación es: ").append(code).append("\n");
        switch(option) {
            case 1 -> {
                sb.append("Te ayudará a finalizar el proceso de registro, mantenlo seguro y no lo compartas con nadie.");
                break;
            }
            case 2 -> {
                sb.append("Te ayudará a actualizar el dispositivo relacionado a tu cuenta, mantenlo seguro y no lo compartas con nadie.");
                sb.append("\nSi necesitas ayuda, puedes comunicarte con: ").append(supportMoneyFy);
                break;
            }
            case 3 -> {
                sb.append("Te ayudará a restablecer la contraseña de tu cuenta de usuario, mantenlo seguro y no lo compartas con nadie");
                break;
            }
            default -> {
                sb.append("Te ayudará a finalizar procesos que necesitan confirmación, mantenlo seguro y no lo compartas con nadie.");
                break;
            }
        }
        sb.append("\n\nEste es un mensaje automático, por favor no lo responda.");
        return sb.toString();
    }

    // CÓDIGO PROPUESTO PARA MAYOR SEGURIDAD - NO IMPLEMENTADO
    // public void sendSecurityCodeToUser(String[] toUsers, List<String> securityCodes) {
    //     try {
    //         this.sendEmail(toUsers, subjectSecurityCode, Map.of("codes", securityCodes), templateSecurityCode);
    //     } catch(Exception e) {
    //         StringBuilder sb = new StringBuilder("Se han creado tus códigos de seguridad exitosamente. Estos te permitirán realizar procesos relevantes de la aplicación, agregando una capa más de seguridad a tu cuenta. Una vez que utilices alguno de ellos y finalices un proceso, no podrás volver a utilizar el mismo código.");
    //         sb.append("\n\nAsegúrate de mantenerlos a salvo:\n\n");
    //         for(String code : securityCodes) {
    //             sb.append(code).append("\n");
    //         }
    //         this.testEmail(toUsers, subjectSecurityCode, sb.toString());
    //     }
    // }

}
