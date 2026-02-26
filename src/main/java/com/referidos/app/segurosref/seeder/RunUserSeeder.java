package com.referidos.app.segurosref.seeder;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.helpers.UserHelper;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.repositories.WhiteListRepository;

// Clase que se comporta como servicio, al levantarse la aplicación para inyectar la data por defecto
@Component
public class RunUserSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReferredRepository referredRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private WhiteListRepository whiteListRepository;

    @Autowired
    private PasswordEncoder pwdEncoder;

    @Autowired
    private UserHelper userHelper;

    // PROCESO QUE SE EJECUTA AL LEVANTARSE LA APLICACIÓN
    @Override
    public void run(String... args) throws Exception {
        String seededUsers = userHelper.seedTestUsers(userRepository, referredRepository, deviceRepository, whiteListRepository, pwdEncoder);
        if(seededUsers == null) {
            LOGGER_MESSAGES.info("Test User Message: se han podido registrar los usuarios");
        } else {
            LOGGER_MESSAGES.info("Test User Message: " + seededUsers);
        }
    }

    // SEEDER PARA USUARIOS DE PRUEBA
    public static List<String> testUsers() {
        return List.of("user.test.appstore@gmail.com");
    }

    // SABER SI EL USUARIO ES UN USUARIO DE PRUEBA
    public static boolean isTestUser(String emailAuth) {
        List<String> seededUsers = testUsers();
        for(String seededUser : seededUsers) {
            if(seededUser.equals(emailAuth)) {
                return true;
            }
        }
        return false;
    }

    // SEEDER PARA USUARIOS POR DEFECTO
    public static List<String> defaultUsers() {
        return List.of("eliu.martineez@gmail.com",
                "gottafindshape@gmail.com",
                "nuser.random01@gmail.com");
    }

    // SABER SI EL USUARIO ES UN USUARIO POR DEFECTO
    public static boolean isDefaulUser(String emailAuth) {
        List<String> seededUsers = defaultUsers();
        for(String seededUser : seededUsers) {
            if(seededUser.equals(emailAuth)) {
                return true;
            }
        }
        return false;
    }

}
