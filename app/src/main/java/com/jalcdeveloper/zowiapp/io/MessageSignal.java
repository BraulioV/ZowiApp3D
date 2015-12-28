package com.jalcdeveloper.zowiapp.io;

public class MessageSignal {

    Object lock = new Object();

    public void doWait(){
        synchronized (lock){
            try{
                lock.wait();
            }catch (Exception ex){}
        }
    }

    public void doNotify(){
        synchronized (lock){
            lock.notify();
        }
    }

}
