package com.jalcdeveloper.zowiapp.io;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


class ZowiService extends Service {

    LinkedList<ZowiCommand> mCommandQueue = new LinkedList<ZowiCommand>();
    Executor mCommandExecutor = Executors.newSingleThreadExecutor();
    Semaphore mCommandLock = new Semaphore(1,true);

    private final IBinder mZowiServiceBinder = new ZowiServiceLocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mZowiServiceBinder;
    }

    public class ZowiServiceLocalBinder extends Binder{
        public ZowiService getService() {
            return ZowiService.this;
        }
    }


    public void queueCommand(ZowiCommand command){
        synchronized (mCommandQueue){
            mCommandQueue.add(command);
            ExecuteZowiCommandRunnable runnable = new ExecuteZowiCommandRunnable(command);
            mCommandExecutor.execute(runnable);

        }
    }

    protected void dequeueCommand(){
        mCommandQueue.pop();
        mCommandLock.release();
    }


    interface ZowiCommand{
        void execute();
    }

    class ExecuteZowiCommandRunnable implements Runnable{

        ZowiCommand command;

        public ExecuteZowiCommandRunnable(ZowiCommand command) {
            this.command = command;
        }

        @Override
        public void run() {

            mCommandLock.acquireUninterruptibly();
            this.command.execute();

        }
    }

}