package com.tmung.miniminder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import javax.crypto.SecretKey;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<SecretKey> aesKey = new MutableLiveData<>();

    public void setAesKey(SecretKey key) {
        aesKey.setValue(key);
    }

    public LiveData<SecretKey> getAesKey() {
        return aesKey;
    }
}

