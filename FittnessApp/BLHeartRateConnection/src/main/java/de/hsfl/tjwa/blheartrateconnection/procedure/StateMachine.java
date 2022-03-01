package de.hsfl.tjwa.blheartrateconnection.procedure;

import android.os.Handler;

/**
 * Übernommen aus BtRfServerClient
 *
 *  Statemachine Klasse
 *
 *  wird im Projekt SmParkingMeter2 gepflegt.
 *
 *  History:
 *      07.12.15 tas Erstellung
 *
 */

public class StateMachine extends Handler {
    private static final String TAG = "hsflStateMachine";

    @Override
    public void handleMessage(android.os.Message message) {
        theBrain(message);
    }           //##0b

    /**
     * virtual method, must be overwritten in subclass
     * @param message
     */
    void theBrain(android.os.Message message){
    }

    protected void setTimer(int messageType, long durationMs){
        android.os.Message msg = new android.os.Message();
        msg.what = messageType;
        msg.arg1 = 0;
        msg.arg2 = 0;
        sendMessageDelayed(msg, durationMs);                                                //##4a
    }

    protected void stopTimer(int messageType){
        removeMessages(messageType);                                                        //##4b
    }

    public void sendSmMessage(int messageType, int arg1, int arg2, Object obj){
        // PrintData d = new PrintData(3);                                                   //##3a
        android.os.Message msg = new android.os.Message();                                   //##0a
        msg.what = messageType;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        if ( obj != null ) {
            msg.obj = obj;
        }
        this.sendMessage(msg);
    }

}