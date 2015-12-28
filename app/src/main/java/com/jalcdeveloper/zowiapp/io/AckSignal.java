package com.jalcdeveloper.zowiapp.io;

/**
 * Created by jalucenyo on 6/12/15.
 */
public class AckSignal {

    Object lock = new Object();

    public void doWait(long timeout){
        synchronized (lock){
            try{
                lock.wait(timeout);
            }catch (Exception ex){}
        }
    }

    public void doNotify(){
        synchronized (lock){
            lock.notify();
        }
    }

}
