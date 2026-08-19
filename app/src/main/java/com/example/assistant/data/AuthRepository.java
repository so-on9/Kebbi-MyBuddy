package com.example.assistant.data;

import com.example.assistant.data.model.User;

public interface AuthRepository {

    interface UserCallback {
        void onSuccess(User u);
        void onError(Throwable t);
    }

    void signIn(String email, String password, UserCallback cb);

    void signUp(String email, String password, String displayName, UserCallback cb);

    // 之後做人臉登入時可用（暫時可不實作）
    void signInWithFace(String faceId, UserCallback cb);

    void signOut();

    boolean isLoggedIn();

    String getToken();

    User getCachedUser();
}
