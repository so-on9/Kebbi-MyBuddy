package com.example.assistant.data;

import com.example.assistant.data.model.User;

public interface ProfileRepository {

    interface ProfileCb {
        void onSuccess(User u);
        void onError(Throwable t);
    }

    void getProfile(String userId, ProfileCb cb);

    void updateProfile(User u, ProfileCb cb);
}
