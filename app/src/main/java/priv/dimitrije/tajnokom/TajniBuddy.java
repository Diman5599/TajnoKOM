package priv.dimitrije.tajnokom;

import androidx.annotation.NonNull;

import org.pjsip.pjsua2.Buddy;

public class TajniBuddy extends Buddy {



    @Override
    public void onBuddyState() {
        super.onBuddyState();
        try {
            //proverava da li je server prihvatio da obradi zahtev
            if (this.getInfo().getSubState() == 4) {
                //obavestava nit koja ceka prihvatanje zahteva
                synchronized (this){
                    this.notify();
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public String toString() {
        try {
            return this.getInfo().getUri();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "buddy exception";
    }

    @Override
    protected void finalize() {
        this.delete();
        super.finalize();
        System.out.println("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF: MyBuddy finalized!\n");
    }
}
