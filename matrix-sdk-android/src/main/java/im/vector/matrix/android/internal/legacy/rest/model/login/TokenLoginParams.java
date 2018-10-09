package im.vector.matrix.android.internal.legacy.rest.model.login;


public class TokenLoginParams extends LoginParams {
    public String user;
    public String token;
    public String txn_id;

    // A display name to assign to the newly-created device
    public String initial_device_display_name;


    public TokenLoginParams() {
        type = "m.login.token";
    }
}
