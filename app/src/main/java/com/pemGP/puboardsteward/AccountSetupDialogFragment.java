package com.pemGP.puboardsteward;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.app.DialogFragment;
import android.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by Florian on 30.07.2014.
 */
public class AccountSetupDialogFragment extends DialogFragment {
    // UI Members
    private EditText mEditTextName;
    private EditText mEditTextTopup;

    private mAccount mAcc;

    // Tag for Intent
    public static final String EXTRA_ACCOUNT = "com.pemGP.PuboardSteward.account";
    public static final String EXTRA_EDITABLE = "com.pemGP.PuboardSteward.editable";

    public static final String TAG = "PuboardSteward.AccountSetupDialogFragment";

    public static AccountSetupDialogFragment newInstance(mAccount mAcc) throws IllegalArgumentException {
        // can only be called with proper ID!
        if (mAcc.getAccID().equals("") || mAcc == null)
            throw new IllegalArgumentException("No account ID passed or null account passed");

        Bundle args = new Bundle();
        args.putSerializable(EXTRA_ACCOUNT, mAcc);

        AccountSetupDialogFragment fragment = new AccountSetupDialogFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public static AccountSetupDialogFragment newInstance(mAccount mAcc, boolean editable) throws IllegalArgumentException {
        AccountSetupDialogFragment fragment = newInstance(mAcc);

        Bundle args = fragment.getArguments();
        args.putBoolean(EXTRA_EDITABLE,editable);
        fragment.setArguments(args);

        return fragment;

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAcc = getArguments() != null ? (mAccount) getArguments().getSerializable(EXTRA_ACCOUNT) : null;
        // take either false from EDITABLE or set true as default.
        boolean editable = getArguments().getBoolean(EXTRA_EDITABLE, true);

        if (mAcc == null)
            throw new IllegalArgumentException("No account passed for creation");

        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_accountsetup, null);
        mEditTextName = (EditText) v.findViewById(R.id.edit_name);
        mEditTextTopup = (EditText) v.findViewById(R.id.edit_topup);

        if (!mAcc.getAccName().equals("")) {
            // if account exists
            mEditTextName.setText(mAcc.getAccName());
            mEditTextName.setEnabled(false);
            if (editable) {
                mEditTextTopup.setHint("Currently: £" + mAcc.getAccMoney() + "0. How much to Top-up?");
                mEditTextTopup.requestFocus();
            } else {
                mEditTextTopup.setText("Currently: £" + mAcc.getAccMoney() + "0");
                mEditTextTopup.setEnabled(false);
                mEditTextTopup.setTextColor(Color.GREEN);
                mEditTextTopup.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30); // TODO: adjust!
            }
        } else {
            // Show soft keyboard automatically
            mEditTextName.requestFocus();
        }

        AlertDialog a = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setTitle(R.string.account_setup_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        double topup = 0;
                        try {
                            topup = Double.parseDouble(mEditTextTopup.getText().toString());
                        } catch (Exception e) {
                            Log.e(TAG, e + "\nerror while parsing topup amount -> cancel");
                            return;
                        }
                        if (mAcc.getAccName().equals("")) {
                            String newName = mEditTextName.getText().toString();
                            if (newName.equals("")) {
                                makeToast("Aborted: Name missing");
                            }
                            mAcc.setAccName(newName);
                        } else if (topup == 0) {   // account exists && no topup -> exit
                            return;
                        }
                        // from here: Name, ID and topup amount exists.

                        // Display message in DrinksPagerFragment, if in foreground and use as listener
                        Fragment fragment = getActivity().getFragmentManager().findFragmentById(R.id.container);
                        DrinksPagerFragment listener = null;
                        if (fragment.getClass() == DrinksPagerFragment.class) {
                            listener = (DrinksPagerFragment) fragment;
                            listener.displayConfirmTransaction(DrinksPagerFragment.TransactionState.TOPUP_PENDING, topup, mAcc);
                        }
                        AccountStore.get(getActivity()).pushTransaction(getActivity(), mAcc, topup,
                                getString(R.string.transaction_topup), null, listener);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        a.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return a;
    }

    private void sendResult(int resultCode){
        Intent i = new Intent();
        i.putExtra(EXTRA_ACCOUNT, mAcc);

    }

    protected void makeToast(String s){
        Toast.makeText(getActivity(), s, Toast.LENGTH_LONG).show();
    }

}
