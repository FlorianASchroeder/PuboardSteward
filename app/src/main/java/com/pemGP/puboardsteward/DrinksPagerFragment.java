package com.pemGP.puboardsteward;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DrinksPagerFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class DrinksPagerFragment extends Fragment implements DrinksFragment.OnDrinksFragmentInteractionListener,
    DrinkStore.OnDrinkStoreUpdatedListener, AccountStore.OnTransactionCompletedListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_SECTION_NUMBER = "DrinksPagerFragment.Position";

    private static final String STATE_SALDO = "DrinksPagerFragment.Saldo";
    private static final String STATE_HISTORY = "DrinksPagerFragment.History";
    private static final String STATE_STATUS = "DrinksPagerFragment.Status";
    private static final String STATE_TRANSACTION = "DrinksPagerFragment.Transaction";

    public static final String TAG = "PuboardSteward.DrinksPagerFragment";

    // UI components
    private TextView mTextViewSaldo;
    private TextView mTextViewHistory;
    private TextView mTextViewStatus;
    private ViewPager mViewPager;

    DrinksFragmentPagerAdapter mPagerAdapter;

    // Model variables
    private double mSaldo;
    private String mHistory;
    private String mStatus;
    private ArrayList<Drink> mPendingTransactions;

    enum TransactionState {
        TOPUP_COMPLETE, TOPUP_PENDING, PURCHASE_COMPLETE, PURCHASE_PENDING, NO_MONEY, ERROR,
        UNDO_COMPLETE, UNDO_ERROR
    }

    // Constructor
    public static DrinksPagerFragment newInstance(int section) {
        DrinksPagerFragment fragment = new DrinksPagerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, section);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    public DrinksPagerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // initialize Displayed TextView Content
        mStatus = "";
        mHistory = "";
        mSaldo = 0;
        mPendingTransactions = new ArrayList<Drink>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v=inflater.inflate(R.layout.fragment_drinks_pager, container, false);
        mViewPager=(ViewPager) v.findViewById(R.id.pager);
        // Add the pager adapter
        mPagerAdapter = (DrinksFragmentPagerAdapter) buildAdapter();
        mViewPager.setAdapter(mPagerAdapter);

        mTextViewSaldo = (TextView) v.findViewById(R.id.textViewSaldo);
        mTextViewSaldo.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Tap on Saldo field refreshes/reset view!
                mTextViewSaldo.setTextColor(Color.WHITE);
                mTextViewStatus.setTextColor(Color.WHITE);
                mSaldo = 0;
                mHistory = "";
                mStatus = "Select drink";
                for (int i = 0; i< mPendingTransactions.size(); i++) {
                    Drink d = mPendingTransactions.get(i);
                    mSaldo += d.getPrice();
                    mHistory += d.toString("history")+"\n";
                }
                updateTextViews();
            }
        });
        mTextViewStatus = (TextView) v.findViewById(R.id.textViewStatus);

        mTextViewHistory = (TextView) v.findViewById(R.id.textViewHistory);
        mTextViewHistory.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // remove last drink from list.
                if (mPendingTransactions.isEmpty())
                    return;
                mPendingTransactions.remove(mPendingTransactions.size()-1);
                // rebuild Saldo and History
                mSaldo = 0;
                mHistory = "";
                mStatus = "Last drink removed";
                mTextViewSaldo.setTextColor(Color.WHITE);
                mTextViewStatus.setTextColor(Color.WHITE);
                for (int i = 0; i< mPendingTransactions.size(); i++) {
                    Drink d = mPendingTransactions.get(i);
                    mSaldo += d.getPrice();
                    mHistory += d.toString("history")+"\n";
                }
                if (mPendingTransactions.isEmpty())
                    mStatus = "Select drink";
                updateTextViews();
            }
        });
        mTextViewHistory.setMovementMethod(new ScrollingMovementMethod()); // makes Text view scrollable.

        return(v);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Obtain saved values
        if (savedInstanceState != null){
            mPendingTransactions = (ArrayList<Drink>) savedInstanceState.getSerializable(STATE_TRANSACTION);
            mSaldo = savedInstanceState.getDouble(STATE_SALDO);
            mHistory = savedInstanceState.getString(STATE_HISTORY);
            mStatus = savedInstanceState.getString(STATE_STATUS);
        }

        updateTextViews();
        ((NavDrawActivity) getActivity()).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onStart() {
        super.onStart();
        DrinkStore.get(getActivity()).addOnDrinkStoreUpdatedListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        DrinkStore.get(getActivity()).removeOnDrinkStoreUpdatedListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.drinkspager_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.options_refresh_drinks:
                DrinkStore.get(getActivity()).refreshDrinks(getActivity());
                break;
            case R.id.options_edit_drinksheet:
                Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(CommonUtilities.SPREADSHEET_URL));
                sendIntent.setPackage("com.google.android.apps.docs.editors.sheets");

                // could also use PackageManager.
                //PackageManager pm = getPackageManager();
                //sendIntent = pm.getLaunchIntentForPackage("com.google.android.apps.docs.editors.sheets");
                //sendIntent.setAction(Intent.ACTION_VIEW);
                //sendIntent.removeCategory(Intent.CATEGORY_LAUNCHER);

                sendIntent.addCategory(Intent.CATEGORY_DEFAULT);
                Log.i(TAG,"Send Intent: "+sendIntent);
                startActivityForResult(sendIntent, NavDrawActivity.REQUEST_CODE_EDIT_SHEET);
                break;
            case R.id.options_undo_last_transaction:
                AlertDialog a = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.dialog_confirm_undo_title)
                        .setMessage(R.string.dialog_confirm_undo_message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                undoLastTransaction();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();
                a.show();
        }
        return super.onOptionsItemSelected(item);
    }

    public void undoLastTransaction() {
        ServerUtilities.undoLastTransaction(getActivity(), this);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_HISTORY, mHistory);
        outState.putString(STATE_STATUS, mStatus);
        outState.putDouble(STATE_SALDO, mSaldo);
        outState.putSerializable(STATE_TRANSACTION, mPendingTransactions);
        super.onSaveInstanceState(outState);
    }

    private void updateTextViews(){
        mTextViewStatus.setText(mStatus);
        mTextViewSaldo.setText("£"+mSaldo+"0");
        mTextViewHistory.setText(mHistory);
    }

    public boolean hasSaldo(){
        return !mPendingTransactions.isEmpty();
    }

    public void processTransaction(mAccount a) {
        // is called upon NFC intent and hasSaldo() checks
        //int size = a.getAccName().length();
        if (!mPendingTransactions.isEmpty() && (a.getAccMoney() >= mSaldo || a.getAccName().startsWith("GPC "))) {
            // push transaction with mPendingTransactions for inventory
            AccountStore.get(getActivity()).pushTransaction(getActivity(), a, mSaldo, getString(R.string.transaction_purchase),
                    mPendingTransactions, this);

            // reset values here rather than in callback upon GCM confirmation.
            displayConfirmTransaction(TransactionState.PURCHASE_PENDING, mSaldo, a);
            // thus can process multiple transaction simultaneously!
        } else {
            //makeToast("Not enough money, Please topup!");
            displayConfirmTransaction(TransactionState.NO_MONEY, 0, a);
            // TODO: offer to top-up in place!
        }
    }

    public void displayConfirmTransaction(TransactionState state, double amount, mAccount account){
        // clear all pending transaction as a otherwise continuing is difficult!
        mPendingTransactions.clear();
        switch (state) {
            case TOPUP_COMPLETE:
                mStatus = getString(R.string.pagerstatus_topup);
                mSaldo = account.getAccMoney();
                mHistory = "";
                mTextViewSaldo.setTextColor(Color.GREEN);
                break;
            case PURCHASE_COMPLETE:
                // transaction completed
                mStatus = getString(R.string.pagerstatus_paid);
                mSaldo = account.getAccMoney();
                mHistory = "";
                mTextViewSaldo.setTextColor(Color.GREEN);
                break;
            case PURCHASE_PENDING:
                // purchase in progress
                if (account == null)
                    throw new IllegalArgumentException("Account needed!");
                mStatus = getString(R.string.pagerstatus_payment_pending, account.getAccName());
                mSaldo = amount;
                mTextViewSaldo.setTextColor(Color.YELLOW);
                break;
            case TOPUP_PENDING:
                // topup in progress
                mStatus = getString(R.string.pagerstatus_topup_pending);
                mSaldo = amount;
                mTextViewSaldo.setTextColor(Color.YELLOW);
                mHistory = "";
                break;
            case NO_MONEY:
                // not enough money
                mStatus = getString(R.string.pagerstatus_no_money);
                // reset saldo and history if no money
                mSaldo = account.getAccMoney();
                mHistory = "";
                mTextViewSaldo.setTextColor(Color.RED);
                break;
            case ERROR:
                // transaction error
                mStatus = getString(R.string.pagerstatus_error_repeat);
                mTextViewStatus.setTextColor(Color.RED);
                break;
            case UNDO_COMPLETE:
                // Last transaction is undone
                mStatus = getString(R.string.pagerstatus_undo_complete);
                if (account != null) {
                    mSaldo = account.getAccMoney();
                } else {
                    mSaldo = amount;
                }
                mTextViewSaldo.setTextColor(Color.GREEN);
                mHistory = "";
                break;
            case UNDO_ERROR:
                // Undo not successful
                mStatus = getString(R.string.pagerstatus_undo_error);
                mSaldo = 0;
                mTextViewSaldo.setTextColor(Color.RED);
                mHistory = "";
                break;
        }
        updateTextViews();
    }
    protected void makeToast(String s){
        Toast.makeText(getActivity(), s, Toast.LENGTH_LONG).show();
    }

    private PagerAdapter buildAdapter() {
        return(new DrinksFragmentPagerAdapter(getActivity(), getChildFragmentManager()));
    }

    @Override
    public void onDrinkStoreUpdated() {
        mPagerAdapter.refreshCategories(getActivity());
        mPagerAdapter.notifyDataSetChanged();
        Log.i(TAG, "Refresh of DrinksPager called.");
    }

    @Override
    public void onDrinksFragmentInteraction(Drink d) {
        mPendingTransactions.add(d);
        if (mPendingTransactions.size() == 1) {
            mSaldo = 0;                 // do reset here if new transaction!
            mHistory = "";
            mStatus = "";
            mTextViewSaldo.setTextColor(Color.WHITE);
            mTextViewStatus.setTextColor(Color.WHITE);
        }
        mSaldo += d.getPrice();
        mHistory += d.toString("history")+"\n";
        mStatus = d.getName();

        updateTextViews();
    }

    @Override
    public void onTransactionCompleted(double amountConfirmed, mAccount acc) {
        //mPendingTransactions.clear();
        if (amountConfirmed < 0) {
            displayConfirmTransaction(TransactionState.PURCHASE_COMPLETE, -amountConfirmed, acc);
        } else {
            displayConfirmTransaction(TransactionState.TOPUP_COMPLETE, amountConfirmed, acc);
        }
    }
}
