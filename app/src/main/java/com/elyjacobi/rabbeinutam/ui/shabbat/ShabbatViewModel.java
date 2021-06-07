package com.elyjacobi.rabbeinutam.ui.shabbat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ShabbatViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ShabbatViewModel() {
        mText = new MutableLiveData<>();
    }

    public LiveData<String> getText() {
        return mText;
    }

    public void setText(String s) {
        mText.setValue(s);
    }
}