package com.tmung.miniminder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import javax.crypto.SecretKey;

// this class is used for sharing data between different fragments/activities
public class SharedViewModel extends ViewModel {
    // MutableLiveData to hold SecretKey (can be changed)
    private final MutableLiveData<SecretKey> aesKey = new MutableLiveData<>();

    // public method to set AES key in MutableLiveData object
    public void setAesKey(SecretKey key) {
        aesKey.setValue(key);
    }

    // public method to get the AES key stored in the MutableLiveData object
    public LiveData<SecretKey> getAesKey() {
        return aesKey;
    }
}

