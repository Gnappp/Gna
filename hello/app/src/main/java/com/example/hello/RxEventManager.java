package com.example.hello;

import rx.subjects.PublishSubject;

/**
 * Created by 진화 on 2017-07-24.
 */

public class RxEventManager {

    private static RxEventManager mInstance;

    private PublishSubject<Object> mData;

    private RxEventManager(){
        mData = PublishSubject.create();
    }

    public static RxEventManager getInstance(){
        if(mInstance == null) {
            mInstance = new RxEventManager();
        }
        return mInstance;
    }

    public void sendData(Object data){
        mData.onNext(data);
    }

    public rx.Observable<Object> getObjectObservable(){
        return mData;
    }

}
