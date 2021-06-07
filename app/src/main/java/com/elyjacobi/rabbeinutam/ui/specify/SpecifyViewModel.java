package com.elyjacobi.rabbeinutam.ui.specify;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SpecifyViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public SpecifyViewModel() {
        mText = new MutableLiveData<>();
    }

    public LiveData<String> getText() {
        return mText;
    }

    public void setText(String s) {
        mText.setValue(s);
    }
}