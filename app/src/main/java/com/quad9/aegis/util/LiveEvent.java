package com.quad9.aegis.util;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

public class LiveEvent<T> extends MutableLiveData<T> {
    private static final String TAG = "LiveEvent";

    private final AtomicBoolean pendingEvent = new AtomicBoolean(false);

    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        super.observe(owner, t -> {
            if (pendingEvent.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }

    public void setValue(T t) {
        pendingEvent.set(true);
        super.setValue(t);
    }

    public void set() {
        setValue(null);
    }
}
