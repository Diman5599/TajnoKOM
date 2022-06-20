package priv.dimitrije.tajnokom;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AccountInfo;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnInstantMessageParam;
import org.pjsip.pjsua2.OnInstantMessageStatusParam;
import org.pjsip.pjsua2.OnRegStateParam;

import java.util.Base64;

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
        String msgText = prm.getMsgBody();
        try{
            Base64.getDecoder().decode(msgText);
            prm.setMsgBody(App.getInstance().getEncryptor().decrypt(msgText));
        }catch (Exception | Error e){
            e.printStackTrace();
        }

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

    @Override
    public void onIncomingCall(OnIncomingCallParam prm) {
        super.onIncomingCall(prm);
        Call call = new MyCall(this, prm.getCallId());
        CallOpParam callOpParam = new CallOpParam(true);
        callOpParam.setStatusCode(200);

        App.activeCall = call;
        try {
            call.answer(callOpParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
