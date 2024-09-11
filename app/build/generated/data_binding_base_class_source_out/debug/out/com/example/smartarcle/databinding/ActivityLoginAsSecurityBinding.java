// Generated by view binder compiler. Do not edit!
package com.example.smartarcle.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.smartarcle.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityLoginAsSecurityBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final Button btnLoginSec;

  @NonNull
  public final TextInputEditText edLoginEmailSec;

  @NonNull
  public final TextInputEditText edLoginPasswordSec;

  @NonNull
  public final TextInputLayout etlEmailSec;

  @NonNull
  public final TextInputLayout etlPwdSec;

  @NonNull
  public final TextView messageLogin;

  @NonNull
  public final ProgressBar progressBar;

  @NonNull
  public final TextView tvForgotpwSec;

  @NonNull
  public final TextView tvLoginEmail;

  @NonNull
  public final TextView tvLoginPassword;

  private ActivityLoginAsSecurityBinding(@NonNull ConstraintLayout rootView,
      @NonNull Button btnLoginSec, @NonNull TextInputEditText edLoginEmailSec,
      @NonNull TextInputEditText edLoginPasswordSec, @NonNull TextInputLayout etlEmailSec,
      @NonNull TextInputLayout etlPwdSec, @NonNull TextView messageLogin,
      @NonNull ProgressBar progressBar, @NonNull TextView tvForgotpwSec,
      @NonNull TextView tvLoginEmail, @NonNull TextView tvLoginPassword) {
    this.rootView = rootView;
    this.btnLoginSec = btnLoginSec;
    this.edLoginEmailSec = edLoginEmailSec;
    this.edLoginPasswordSec = edLoginPasswordSec;
    this.etlEmailSec = etlEmailSec;
    this.etlPwdSec = etlPwdSec;
    this.messageLogin = messageLogin;
    this.progressBar = progressBar;
    this.tvForgotpwSec = tvForgotpwSec;
    this.tvLoginEmail = tvLoginEmail;
    this.tvLoginPassword = tvLoginPassword;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityLoginAsSecurityBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityLoginAsSecurityBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_login_as_security, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityLoginAsSecurityBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btn_login_sec;
      Button btnLoginSec = ViewBindings.findChildViewById(rootView, id);
      if (btnLoginSec == null) {
        break missingId;
      }

      id = R.id.ed_login_email_sec;
      TextInputEditText edLoginEmailSec = ViewBindings.findChildViewById(rootView, id);
      if (edLoginEmailSec == null) {
        break missingId;
      }

      id = R.id.ed_login_password_sec;
      TextInputEditText edLoginPasswordSec = ViewBindings.findChildViewById(rootView, id);
      if (edLoginPasswordSec == null) {
        break missingId;
      }

      id = R.id.etl_email_sec;
      TextInputLayout etlEmailSec = ViewBindings.findChildViewById(rootView, id);
      if (etlEmailSec == null) {
        break missingId;
      }

      id = R.id.etl_pwd_sec;
      TextInputLayout etlPwdSec = ViewBindings.findChildViewById(rootView, id);
      if (etlPwdSec == null) {
        break missingId;
      }

      id = R.id.message_login;
      TextView messageLogin = ViewBindings.findChildViewById(rootView, id);
      if (messageLogin == null) {
        break missingId;
      }

      id = R.id.progress_bar;
      ProgressBar progressBar = ViewBindings.findChildViewById(rootView, id);
      if (progressBar == null) {
        break missingId;
      }

      id = R.id.tv_forgotpw_sec;
      TextView tvForgotpwSec = ViewBindings.findChildViewById(rootView, id);
      if (tvForgotpwSec == null) {
        break missingId;
      }

      id = R.id.tv_login_email;
      TextView tvLoginEmail = ViewBindings.findChildViewById(rootView, id);
      if (tvLoginEmail == null) {
        break missingId;
      }

      id = R.id.tv_login_password;
      TextView tvLoginPassword = ViewBindings.findChildViewById(rootView, id);
      if (tvLoginPassword == null) {
        break missingId;
      }

      return new ActivityLoginAsSecurityBinding((ConstraintLayout) rootView, btnLoginSec,
          edLoginEmailSec, edLoginPasswordSec, etlEmailSec, etlPwdSec, messageLogin, progressBar,
          tvForgotpwSec, tvLoginEmail, tvLoginPassword);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
