package priv.dimitrije.tajnokom;

import org.pjsip.pjsua2.BuddyConfig;

public class MyBuddyCfg extends BuddyConfig {
    @Override
    protected void finalize() {
        System.out.println("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF bcfg!\n");
        super.finalize();
    }
}
