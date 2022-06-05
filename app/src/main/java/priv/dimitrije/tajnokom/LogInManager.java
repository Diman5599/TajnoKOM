package priv.dimitrije.tajnokom;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;

public class LogInManager {
    private String domainName;
    private String username;
    private String password;

    public LogInManager(){

    }

    public boolean logIn(RELogInCreds reLogInCreds){

        domainName = reLogInCreds.domainName;
        password = reLogInCreds.password;
        username = reLogInCreds.username;

        try{
            App.getInstance().endpoint.libRegisterThread(Thread.currentThread().getName());
        }catch (Exception e){
            e.printStackTrace();
        }

        //instanciranje korisnickog naloga
        App.getInstance().usrAccount = new MyAccount();

        //konfiguracija korisničkog naloga
        AccountConfig accfg = new AccountConfig();

        //postavljanje adrese servera za registraciju i sip adrese korisnika
        accfg.getRegConfig().setRegistrarUri("sip:" + reLogInCreds.domainName);
        accfg.getRegConfig().setTimeoutSec(60);
        String s = "sip:" + reLogInCreds.username + "@" + reLogInCreds.domainName;
        accfg.setIdUri(s);

        //inicijalizovanje autentikacionih podataka korisnika
        App.aci = new AuthCredInfo("digest", "*", reLogInCreds.username, 0, reLogInCreds.password);
        accfg.getSipConfig().getAuthCreds().add(App.aci);
        accfg.getNatConfig().setIceEnabled(true);

        // kreiranje korisnickog naloga na osnovu konfiguracije sa log-in aktivnosti
        try {
            App.getInstance().usrAccount.create(accfg);

            Thread.sleep(100);

            App.accInfo = App.getInstance().usrAccount.getInfo();;

            //nit čeka da se nalog registruje
            int timeout = 0;
            while(App.accInfo.getRegStatus() != 200 && timeout < 5 && App.accInfo.getRegStatus() != 403){
               // App.usrAccount.setRegistration(false);
                try {
                    Thread.sleep(500);
                    App.getInstance().usrAccount.setRegistration(true);
                }catch (Exception e){
                    //ukoliko server nije vidljiv ovde se javlja greska
                    timeout++;
                    e.printStackTrace();
                }
                //ako se desi "Request merged" takodje povecati timeout cntr
                if(App.accInfo.getRegStatus() == 482) timeout++;
                Thread.sleep(2000);
                App.accInfo = App.getInstance().usrAccount.getInfo();
            }
            if (timeout >= 5 || App.accInfo.getRegStatus() == 403){
                App.getInstance().usrAccount.delete();
                //App.getInstance().pjTrash.add(App.aci);
                //App.getInstance().pjTrash.add(accfg);
                App.aci.delete();
                App.aci = null;
                accfg.delete();
                return false;
            }
            //App.getInstance().pjTrash.add(App.aci);
            //App.getInstance().pjTrash.add(accfg);
            App.aci.delete();
            App.aci = null;
            accfg.delete();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        App.getInstance().usrAccount.delete();
        App.aci.delete();
        return false;
    }
}
