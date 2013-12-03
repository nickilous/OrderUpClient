package com.nickilous.orderupclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;

import com.nickilous.OrderInfo;
import com.nickilous.OrderMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;

public class OrderUpClientMainActivity extends Activity {
    private ListView mOrderNumberListView;
    private ArrayAdapter<String> mOrderNumberArrayAdapter;
    private ArrayList<String> mOrderTimeArrayList;
    private Handler mUpdateHandler;

    private OrderMessage    mOrderMessage;

    public static final String TAG = "NsdChat";

    OrderUpConnection mConnection;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOrderNumberListView = (ListView) findViewById(R.id.order_numbers);
        mOrderNumberArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_view_center_layout, R.id.textItem);
        mOrderNumberListView.setAdapter(mOrderNumberArrayAdapter);
        mOrderNumberListView.setOnItemClickListener(new OrderNumberOnItemClickListener());

        mOrderTimeArrayList = new ArrayList<String>();

        mOrderMessage = new OrderMessage();

        mUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String chatLine = msg.getData().getString("msg");

            }
        };

        mConnection = new OrderUpConnection(mUpdateHandler);
        showConnectToServerDialog();



    }

    public void clickConnect(String ipAddress) {

        try {
            mConnection.connectToServer(InetAddress.getByName(ipAddress), 5050);


        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        DialogFragment prev = (DialogFragment)getFragmentManager().findFragmentByTag("connectToServer");
        if (prev != null) {
            DialogFragment df = prev;
            df.dismiss();
        }



    }
    public void clickAddNewOrder(View v){

        if(mConnection.isConnected()){
            mOrderMessage.setAction(OrderMessageType.ADD_ORDER);
            showOrderNumberDialog();
        } else {
            showConnectToServerDialog();
        }
    }


    public void clickAddOrder(OrderMessage orderMessage){
        DialogFragment prev = (DialogFragment)getFragmentManager().findFragmentByTag("orderNumber");
        if (prev != null) {
            DialogFragment df = prev;
            df.dismiss();
        }
        showTimePickerDialog(orderMessage);
    }

    public void showTimePickerDialog(OrderMessage orderMessage) {
        DialogFragment newFragment = new TimePickerFragment(orderMessage);
        newFragment.show(getFragmentManager(), "timePicker");

    }

    public void showConnectToServerDialog(){
        DialogFragment newFragment = new ConnectToServerDialog();
        newFragment.show(getFragmentManager(), "connectToServer");
    }

    public void showOrderNumberDialog(){
        DialogFragment newFragment = new OrderNumberDialog();
        newFragment.show(getFragmentManager(), "orderNumber");
    }

    public void showConfirmationToAddOrder(OrderMessage orderMessage){
        DialogFragment newFragment = new ConfirmationToAddOrderDialog(orderMessage);
        newFragment.show(getFragmentManager(), "confirmationDialog");

    }

    public void showConfirmationToRemoveOrder(OrderMessage orderMessage){

        DialogFragment newFragment = new ConfirmationToDeleteOrderDialog(orderMessage);
        newFragment.show(getFragmentManager(), "confirmationRemovalDialog");


    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener, DialogInterface.OnDismissListener {
        OrderMessage orderMessage;

        public TimePickerFragment(OrderMessage orderMessage){
            this.orderMessage = orderMessage;
        }

        public void onDismiss(DialogInterface dialog){
            ((OrderUpClientMainActivity) getActivity()).showConfirmationToAddOrder(orderMessage);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));

            return timePickerDialog;


        }



        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            String stringMinute;
            String suffix = "am";
            if (minute < 10){
                stringMinute = new String("0" + String.valueOf(minute));
            } else {
                stringMinute = new String(String.valueOf(minute));
            }

            if (hourOfDay > 12){
                hourOfDay -= 12;
                suffix = "pm";
            }
            orderMessage.getOrderInfo().setmOrderTime(String.valueOf(hourOfDay) + ":" + stringMinute
                                                            + " " + suffix);

        }
    }

    public static class ConnectToServerDialog extends DialogFragment{
        public ConnectToServerDialog(){

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.connect_to_server_dialog, container);
            final EditText ipAddressEditText = (EditText) view.findViewById(R.id.ip_address);

            getDialog().setTitle("Connect To Server");

            Button connectBtn = (Button) view.findViewById(R.id.connect_btn);
            connectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String ipAddress = ipAddressEditText.getText().toString();
                    ((OrderUpClientMainActivity) getActivity()).clickConnect(ipAddress);
                }
            });

            return view;
        }
    }

    public class OrderNumberDialog extends DialogFragment{
        public OrderNumberDialog(){

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState){
            final View view = inflater.inflate(R.layout.order_number_dialog, container);
            getDialog().setTitle("Order Number Entry");
            Button addOrderButton = (Button) view.findViewById(R.id.addOrderButton);
            addOrderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText orderInputEditText = (EditText) view.findViewById(R.id.order_input);
                    mOrderMessage.getOrderInfo().setmOrderNumber(orderInputEditText.getText().toString());
                    ((OrderUpClientMainActivity) getActivity()).clickAddOrder(mOrderMessage);
                }
            });


            return view;
        }
    }

    public static class ConfirmationToAddOrderDialog extends DialogFragment{
        OrderMessage orderMessage;
        public ConfirmationToAddOrderDialog(OrderMessage orderMessage){
            this.orderMessage = orderMessage;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Order Information Confirmation")
                    .setMessage("You have entered in an order with an order number of " +
                            orderMessage.getOrderInfo().getmOrderNumber() +
                            " and a time of " + orderMessage.getOrderInfo().getmOrderTime() +
                            " clicking yes will send the information clicking no will clear it.")
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((OrderUpClientMainActivity) getActivity()).sendOrderMessage(orderMessage);
                                    dismiss();
                                }
                            }
                    )
                    .setNegativeButton("No",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dismiss();
                                }
                            }
                    )
                    .create();
        }
    }


    public static class ConfirmationToDeleteOrderDialog extends DialogFragment{
        OrderMessage orderMessage;
        public ConfirmationToDeleteOrderDialog(OrderMessage orderMessage){
            this.orderMessage = orderMessage;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Order Removal Confirmation")
                    .setMessage("Are you sure you want to delete the order " +
                            orderMessage.getOrderInfo().getmOrderNumber() +
                            " with a time of " + orderMessage.getOrderInfo().getmOrderTime())
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((OrderUpClientMainActivity) getActivity()).sendOrderMessage(orderMessage);
                                    dismiss();

                                }
                            }
                    )
                    .setNegativeButton("No",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //TODO: Possibly do something on negative click
                                }
                            }
                    )
                    .create();
        }

    }

    public class OrderNumberOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String itemToRemove = mOrderNumberArrayAdapter.getItem(position);
            OrderMessage orderMessage = new OrderMessage();
            orderMessage.setAction(OrderMessageType.REMOVE_ORDER);

            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setmOrderNumber(itemToRemove);
            orderInfo.setmOrderTime(mOrderTimeArrayList.get(position));

            orderMessage.setOrderInfo(orderInfo);

            showConfirmationToRemoveOrder(orderMessage);




        }
    }

    private void sendOrderMessage(OrderMessage orderMessage) {
        if (orderMessage != null) {
            if (!orderMessage.toString().isEmpty() && orderMessage.getAction()== OrderMessageType.ADD_ORDER) {
                mConnection.sendMessage(orderMessage);
                mOrderNumberArrayAdapter.add(orderMessage.getOrderInfo().getmOrderNumber());
                mOrderTimeArrayList.add(orderMessage.getOrderInfo().getmOrderTime());
                mOrderNumberArrayAdapter.notifyDataSetChanged();
            } else if(orderMessage.getAction() == OrderMessageType.REMOVE_ORDER) {
                mConnection.sendMessage(orderMessage);
                mOrderNumberArrayAdapter.remove(orderMessage.getOrderInfo().getmOrderNumber());
                mOrderNumberArrayAdapter.notifyDataSetChanged();
                mOrderTimeArrayList.remove(orderMessage.getOrderInfo().getmOrderTime());

            }

        }

    }


    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        mConnection.tearDown();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.order_up_client_main, menu);
        return true;
    }

}