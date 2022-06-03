package priv.dimitrije.tajnokom;

import android.content.Intent;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AccountInfo;
import org.pjsip.pjsua2.OnInstantMessageParam;
import org.pjsip.pjsua2.OnInstantMessageStatusParam;
import org.pjsip.pjsua2.OnRegStateParam;

public class MyAccount extends Account {
    public static final String ACTION_PJSIP_IM_RECEIVED = "pjsip_im_received";

    @Override
    public void onRegState(OnRegStateParam prm) {
        super.onRegState(prm);
        try {
            AccountInfo ai = this.getInfo();
            System.out.println((ai.getRegIsActive() ? "Reg. kod: " : "Nije reg. kod: ") + prm.getCode() + "##########################################################################");
            System.out.println(prm.getReason() + " " + ai.getRegIsActive());
        } catch (Exception e) {
            e.printStackTrace();
        }
        prm.delete();
    }

    @Override
    public void onInstantMessage(OnInstantMessageParam prm) {
        super.onInstantMessage(prm);
        System.out.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!STIGLO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" +
                 "\nOd: " + prm.getContactUri() + " | " + prm.getFromUri() +
                "\nTekst: " + prm.getMsgBody()
        );

        App.getInstance().makeNotification(prm);

    }

    @Override
    public void onInstantMessageStatus(OnInstantMessageStatusParam prm) {
        super.onInstantMessageStatus(prm);
        System.out.println(
                "****************************** MESSAGE STATUS ***********************************\n"
                + prm.getCode() + "\n"
                + prm.getReason() + "\n"
                + prm.getRdata().getInfo() + "\n"
        );
        prm.delete();
    }

    @Override
    protected void finalize() {
        super.finalize();
        System.out.println("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF MyAccount finalized!\n");
    }
}
