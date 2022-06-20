package priv.dimitrije.tajnokom;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AudioMedia;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallMediaInfo;
import org.pjsip.pjsua2.OnCallMediaStateParam;
import org.pjsip.pjsua2.OnCallStateParam;
import org.pjsip.pjsua2.pjmedia_type;
import org.pjsip.pjsua2.pjsua_call_media_status;

public class MyCall extends Call {
    protected MyCall(long cPtr, boolean cMemoryOwn) {
        super(cPtr, cMemoryOwn);
    }

    public MyCall(Account acc, int call_id) {
        super(acc, call_id);
    }

    public MyCall(Account acc) {
        super(acc);
    }

    @Override
    public void onCallMediaState(OnCallMediaStateParam prm) {
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!MEDIASTATE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        try {
            CallInfo info = getInfo();
            for(int cntr = 0; cntr < info.getMedia().size(); cntr ++){
                if(info.getMedia().get(cntr).getType() == pjmedia_type.PJMEDIA_TYPE_AUDIO
                        && info.getMedia().get(cntr).getType() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE){
                    AudioMedia audioMedia = getAudioMedia(cntr);
                    App.getInstance().endpoint.audDevManager().getCaptureDevMedia().startTransmit(audioMedia);
                    audioMedia.startTransmit(App.getInstance().endpoint.audDevManager().getPlaybackDevMedia());
                    System.out.println("********************************TRANSMISSION INITIATED****************************");
                }
;            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
