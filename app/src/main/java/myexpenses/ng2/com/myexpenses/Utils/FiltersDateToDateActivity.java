package myexpenses.ng2.com.myexpenses.Utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;

import java.util.Calendar;

import myexpenses.ng2.com.myexpenses.R;

/**
 * Created by Vromia on 15/9/2014.
 * FiltersDateToDateDialog class that extends DialogFragment is used when we want to find expenses or incomes with date between two
 * dates. HistoryActivity calls this class more specific in filters. It also implement the Interface Date from Fragment
 * FiltersDateDialog because we need to send data from FiltersDateDialog to FiltersDateToDateDialog
 *
 */
public class FiltersDateToDateActivity extends FragmentActivity  {

    private Button bOk,bCancel;
    private ImageButton ibFrom , ibTo;
    private EditText etFrom,etTo;
    private Dialog dialog;
    private String from,to;
    //We use the fragment FiltersDateDialog to initialise the fields from and to that are our dates.
    private FiltersDateDialog fdialog;
    //variable flag is used because we want to know which button the user pressed so we initialise the right date
    //and variable expense is to check if this fragment is called for expenses or incomes
    private boolean flag,expense;

    private CalendarDatePickerDialog Cdialog;

/*
    public FiltersDateToDateDialog(boolean expense){
        this.expense=expense;
    }
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filters_datetodate_dialog);

        initUI();
        initListeners();

    }

    /*
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialog=new Dialog(getActivity());
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.filters_datetodate_dialog);

        initUI();
        initListeners();

        return dialog;
    }
*/

    public void initUI(){

        bOk=(Button) findViewById(R.id.bOK);
        bCancel=(Button)findViewById(R.id.bCancel);
        ibFrom=(ImageButton) findViewById(R.id.ibFrom);
        ibTo=(ImageButton) findViewById(R.id.ibTo);
        etFrom=(EditText) findViewById(R.id.etDateFrom);
        etTo=(EditText) findViewById(R.id.etDateTo);
        //initialise the dialog fragment and also set the target fragment to be this parent fragment because we need
        //to take some data from child fragment FiltersDateDialog
      //  fdialog=new FiltersDateDialog(false,expense);
       // fdialog.setTargetFragment(FiltersDateToDateDialog.this,0);

    }


    public void initListeners(){

       ibFrom.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               flag=true;
               Calendar c = Calendar.getInstance();
                Cdialog = CalendarDatePickerDialog.newInstance(dtdListener ,
                       c.get(Calendar.YEAR) , c.get(Calendar.MONTH) , c.get(Calendar.DAY_OF_MONTH));
               Cdialog.show(getSupportFragmentManager() , "Calendar Dialog");
               //fdialog.show(getFragmentManager(),"FiltersDate dialog");

           }
       });

        ibTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag=false;
                Calendar c = Calendar.getInstance();
                Cdialog = CalendarDatePickerDialog.newInstance(dtdListener ,
                c.get(Calendar.YEAR) , c.get(Calendar.MONTH) , c.get(Calendar.DAY_OF_MONTH));
                Cdialog.show(getSupportFragmentManager() , "Calendar Dialog");
              //  fdialog.show(getFragmentManager(),"FiltersDate dialog");

            }
        });

        bOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String DateFrom[]=from.split("-");
                String DateTo[]=to.split("-");
                String reformedDateFrom=DateFrom[2]+"-"+DateFrom[1]+"-"+DateFrom[0];
                String reformedDateTo=DateTo[2]+"-"+DateTo[1]+"-"+DateTo[0];
               //When we press the Ok we check if the user press two dates and also if the dates are ok.
                if(to==null || from==null || reformedDateFrom.compareTo(reformedDateTo)>=0 ){
                    Toast.makeText(FiltersDateToDateActivity.this,"Problem with your dates plz check them and try again",Toast.LENGTH_LONG).show();
                }else{

                    //Intent resultIntent=new Intent();
                    Intent resultIntent=getIntent();
                    resultIntent.putExtra("From",from);
                    resultIntent.putExtra("To",to);
                    setResult(Activity.RESULT_OK,resultIntent);
                    finish();


                /*
                    HistoryActivity activity=(HistoryActivity) getActivity();
                    //If expense is true it means that this fragment is called for expenses so we call the method
                    //saveFiltersDateToDate from HistoryActivity. Otherwise is called for incomes so we call the
                    //method saveIncomeFiltersDateToDate
                    if(expense) {
                        activity.saveFiltersDateToDate(from, to);
                    }else{
                        activity.saveIncomeFiltersDateToDate(from,to);
                    }
                    dialog.dismiss();
                    */

                }

            }
        });
        bCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                finish();
                //Toast.makeText(getActivity(),"Return back no choice happened",Toast.LENGTH_LONG).show();
                //dialog.dismiss();
            }
        });


    }

    private CalendarDatePickerDialog.OnDateSetListener dtdListener=new CalendarDatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(CalendarDatePickerDialog calendarDatePickerDialog, int i, int i2, int i3) {

            String month,day,date;
            if(i2<10){
                month = "0" + i2;
            }else{
                month = String.valueOf(i2);
            }
            if(i3<10){
                day = "0" + i3;
            }else{
                day = String.valueOf(i3);
            }
            date = day + "-" + month + "-" + i;

            if(flag){
                from=date;
                etFrom.setText(from);

            }else{
                to=date;
                etTo.setText(to);
            }

        }
    };


/*
//getDateFromDialogFragment is the method from the interface Date and we need to override it because this class
 //implements Date
    @Override
    public void getDateFromDialogFragment(String date) {
//if the flag is true it means that the user press the Date (from) so we initialise the string variable from and also
//we set the Text of TextView tvFrom. Otherwise we do the same for Date (To).
        if(flag){
            from=date;
            tvFrom.setText(from);
        }else{
            to=date;
            tvTo.setText(to);
        }

    }
    */
}