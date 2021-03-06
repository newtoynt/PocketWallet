package com.ngngteam.pocketwallet.Activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ngngteam.pocketwallet.Adapters.DrawerAdapter;
import com.ngngteam.pocketwallet.Data.CategoryDatabase;
import com.ngngteam.pocketwallet.Data.MoneyDatabase;
import com.ngngteam.pocketwallet.Dialogs.ChangelogDialog;
import com.ngngteam.pocketwallet.Extra.LetterImageView;
import com.ngngteam.pocketwallet.Model.ExpenseItem;
import com.ngngteam.pocketwallet.Model.IncomeItem;
import com.ngngteam.pocketwallet.Model.UserProfile;
import com.ngngteam.pocketwallet.R;
import com.ngngteam.pocketwallet.Services.RecurrentTransactionsService;
import com.ngngteam.pocketwallet.Utils.OverviewBar;
import com.ngngteam.pocketwallet.Utils.RecurrentUtils;
import com.ngngteam.pocketwallet.Utils.SharedPrefsManager;
import com.ngngteam.pocketwallet.Utils.Themer;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import me.grantland.widget.AutofitTextView;

public class OverviewActivity extends AppCompatActivity {

    //request code for the UserDetails sub-Activity
    private static final int USER_DETAILS_SUB_ACTIVITY = 1;
    //SharedPrefsManager object
    private SharedPrefsManager manager;

    //UserProfile object
    private UserProfile profile;

    //View objects for the XML management
    private TextView tvUsername;
    private AutofitTextView tvBalance, tvSavings, tvLastIncomeValue, tvLastExpenseValue, tvLastExpenseDate,
            tvLastIncomeDate, tvBarDate, tvBudgetMessage;
    private LinearLayout llLastExpense, llLastIncome, llSavings;
    private CardView card_message, card_bars, card_last_transactions;
    private LetterImageView livLastExpense, livLastIncome;
    private DrawerLayout drawerLayout;
    private ListView drawer;
    private ActionBarDrawerToggle drawerToggle;

    private OverviewBar bar;

    int firstDayTable[];

    private MoneyDatabase mdb;

    private Cursor cursorLastExpense, cursorLastIncome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Themer.setThemeToActivity(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_overview);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        init();

        //init UI elements
        initUI();

        //set up UI elements
        setUpUI();

        //manage the user profile
        checkUserProfile();

        initListeners();

        try {
            int oldVersion = manager.getPrefsVersion();
            int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            if (oldVersion != versionCode) {
                manager.startEditing();
                manager.setPrefsVersion(versionCode);
                manager.commit();
                new ChangelogDialog().show(getFragmentManager(), "tag");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //if the recurrent transactions service isn't already running , start it
        if (!RecurrentTransactionsService.isRunning)
            RecurrentUtils.startRecurrentService(this);

    }


    private void setUpUI() {

        tvUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OverviewActivity.this, UserDetailsActivity.class));
            }
        });

        //==================================Navigation Drawer=======================================
        //get the string array with the Navigation drawer items
        String drawerItems[] = getResources().getStringArray(R.array.drawer_menu);

        //init the adapter and connect with the View
        DrawerAdapter adapter = new DrawerAdapter(OverviewActivity.this, R.layout.drawer_item, drawerItems);
        drawer.setAdapter(adapter);

        //set on drawer item click listener
        drawer.setOnItemClickListener(drawerClickListener);

        //set shadow for the navigation drawer
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();

            }
        };

        drawerLayout.setDrawerListener(drawerToggle);

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (manager.getPrefsThemeChanged()) {
            startActivity(new Intent(getBaseContext(), OverviewActivity.class));
            OverviewActivity.this.finish();

            manager = new SharedPrefsManager(OverviewActivity.this);
            manager.startEditing();
            manager.setPrefsThemeChanged(false);
            manager.commit();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        //get data from the preference file and init the profile object
        getDataFromSharedPrefs();
        //refresh views according to the profile object values
        refreshUI();

    }

    //checks if a user profile already exists(practically if the app launches for the first time)
    private void checkUserProfile() {

        //store the profile existence in a boolean variable
        boolean isProfile = manager.getPrefsIsProfile();

        //if there isn't a profile already , launch the user details activity to create one
        if (!isProfile) {
            startActivityForResult(new Intent(getApplicationContext(), UserDetailsActivity.class), USER_DETAILS_SUB_ACTIVITY);
        } else {
            //else load the profile data from the shared prefs
            getDataFromSharedPrefs();
            //and refresh the UI elements
            refreshUI();
        }
    }

    //loads the user data from the shared prefs file
    //then initialize the profile object with these variables
    private void getDataFromSharedPrefs() {

        String username = manager.getPrefsUsername();
        float savings = manager.getPrefsSavings();
        float balance = manager.getPrefsBalance();
        String currency = PreferenceManager.getDefaultSharedPreferences(this).getString(getResources().getString(R.string.pref_key_currency), getResources().getString(R.string.pref_currency_default_value));
        String grouping = manager.getPrefsGrouping();
        int dayStart = manager.getPrefsDayStart();

        profile = new UserProfile(username, savings, balance, currency, grouping, dayStart);
    }

    //is called when a sub-Activity with the result code returns
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //each sub-Activity has a different request code
        //here we have only one sub-Activity
        switch (requestCode) {
            case USER_DETAILS_SUB_ACTIVITY:
                //if the user confirmed the creation of the profile
                //get these data from the shared prefs file
                if (resultCode == RESULT_OK) {
                    getDataFromSharedPrefs();
                    //and refresh the UI
                    refreshUI();
                } else {
                    //else close the app(OverviewActivity is the main Activity)
                    finish();
                    break;
                }
        }
    }

    //init some basic variables
    private void init() {

        //shared preferences
        manager = new SharedPrefsManager(this);

        //open money database
        mdb = new MoneyDatabase(OverviewActivity.this);

    }

    //init the UI Views
    private void initUI() {

        //name
        tvUsername = (TextView) findViewById(R.id.tvUsername);

        tvBudgetMessage = (AutofitTextView) findViewById(R.id.tvBudgetMessage);

        tvBalance = (AutofitTextView) findViewById(R.id.tvOverviewBalance);
        tvSavings = (AutofitTextView) findViewById(R.id.tvOverviewSavings);

        llSavings = (LinearLayout) findViewById(R.id.llSavings);

        //=================Message section====================================================
        card_message = (CardView) findViewById(R.id.cardview_message);

        //=================Second section , Bars - Bar heading===================
        card_bars = (CardView) findViewById(R.id.cardview_bars);
        bar = (OverviewBar) findViewById(R.id.overview_bar);
        tvBarDate = (AutofitTextView) findViewById(R.id.tvBarHeading);

        //==================Third Section , Last Transactions=================================
        card_last_transactions = (CardView) findViewById(R.id.cardview_last_transactions);
        card_last_transactions.setVisibility(View.GONE);

        llLastExpense = (LinearLayout) findViewById(R.id.llOverviewLastExpense);
        llLastIncome = (LinearLayout) findViewById(R.id.llOverviewLastIncome);

        tvLastExpenseValue = (AutofitTextView) findViewById(R.id.tvLastExpenseValue);
        tvLastIncomeValue = (AutofitTextView) findViewById(R.id.tvLastIncomeValue);

        tvLastExpenseDate = (AutofitTextView) findViewById(R.id.tvLastExpenseDate);
        tvLastIncomeDate = (AutofitTextView) findViewById(R.id.tvLastIncomeDate);

        livLastExpense = (LetterImageView) findViewById(R.id.livLastExpense);
        livLastIncome = (LetterImageView) findViewById(R.id.livLastIncome);

        //==================Navigation Drawer=================================================
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawer = (ListView) findViewById(R.id.nav_drawer);


    }

    static String getMonthForInt(int num) {
        String month = "January";
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getMonths();
        if (num >= 0 && num <= 11) {
            month = months[num];
        }
        return month;
    }

    //refresh UI according to profile object
    private void refreshUI() {
        tvUsername.setText(profile.getUsername());

        double totalExpenses;
        double totalIncomes;

        //get the prefs grouping and initialize total expense-income
        if (profile.getGrouping().equalsIgnoreCase(getResources().getString(R.string.pref_grouping_monthly))) {
            tvBudgetMessage.setVisibility(View.VISIBLE);
            firstDayTable = new int[]{1, 5, 10, 15, 20, 25};
            totalExpenses = mdb.getTotalForCurrentMonth(true);
            totalIncomes = mdb.getTotalForCurrentMonth(false);
        } else if (profile.getGrouping().equalsIgnoreCase(getResources().getString(R.string.pref_grouping_weekly))) {
            tvBudgetMessage.setVisibility(View.VISIBLE);
            firstDayTable = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
                    Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};
            totalExpenses = mdb.getTotalForCurrentWeek(firstDayTable[profile.getDayStart()], true);
            totalIncomes = mdb.getTotalForCurrentWeek(firstDayTable[profile.getDayStart()], false);
        } else if (profile.getGrouping().equalsIgnoreCase(getResources().getString(R.string.pref_grouping_daily))) {
            tvBudgetMessage.setVisibility(View.VISIBLE);
            totalExpenses = mdb.getDailyTotal(true);
            totalIncomes = mdb.getDailyTotal(false);
        } else {
            tvBudgetMessage.setVisibility(View.GONE);
            totalExpenses = mdb.getTotal(true);
            totalIncomes = mdb.getTotal(false);
        }

        //round values to 2 decimal digits
        totalExpenses = Math.round(totalExpenses * 100) / 100.0;
        totalIncomes = Math.round(totalIncomes * 100) / 100.0;

        //calculate balance/savings and set it to profile object
        double balance = totalIncomes - totalExpenses;

        double budget = manager.getPrefsBudget();
        double dif;
        if (budget < totalExpenses) {
            dif = totalExpenses - budget;
            tvBudgetMessage.setText(dif + " " + profile.getCurrency() + " " +getResources().getString(R.string.over_budget) );
            tvBudgetMessage.setTextColor(getResources().getColor(R.color.red));
        } else {
            dif = budget - totalExpenses;
            tvBudgetMessage.setText(dif + " " + profile.getCurrency() + " " + getResources().getString(R.string.until_budget) );
        }

        double savings = mdb.getTotal(false) - mdb.getTotal(true) - balance + profile.getSavings();

        //round to 2 decimal digits
        balance = Math.round(balance * 100) / 100.0;
        savings = Math.round(savings * 100) / 100.0;

        profile.setBalance((float) balance);
        profile.setSavings((float) savings);

        //check the sign of the balance and set the appropriate color
        String sign = (balance >= 0) ? "+" : "";
        int drawable = (balance >= 0) ? R.drawable.rounded_bounds_green_empty : R.drawable.rounded_bounds_red_empty;
        int paint = (balance >= 0) ? R.color.YellowGreen : R.color.bpRed;
        if (balance == (int) balance) {
            tvBalance.setText(sign + (int) balance + " " + profile.getCurrency());
        } else {
            tvBalance.setText(sign + balance + " " + profile.getCurrency());
        }
        tvBalance.setBackgroundResource(drawable);
        tvBalance.setTextColor(getResources().getColor(paint));

        //check the sign of the savings and set the appropriate color
        sign = (savings >= 0) ? "+" : "";
        drawable = (savings >= 0) ? R.drawable.rounded_bounds_green_empty : R.drawable.rounded_bounds_red_empty;
        paint = (savings >= 0) ? R.color.YellowGreen : R.color.bpRed;
        if (savings == (int) savings) {
            tvSavings.setText(sign + (int) savings + " " + profile.getCurrency());
        } else {
            tvSavings.setText(sign + savings + " " + profile.getCurrency());
        }
        tvSavings.setBackgroundResource(drawable);
        tvSavings.setTextColor(getResources().getColor(paint));

        double total = totalExpenses + totalIncomes;
        //if there is income or expense for the current period
        if (total != 0) {

            //set bar values
            bar.setExpense(totalExpenses);
            bar.setIncome(totalIncomes);

            //set the period of the bars according to the preferred grouping
            if (manager.getPrefsGrouping().equalsIgnoreCase(getResources().getString(R.string.pref_grouping_weekly))) {

                Calendar c = Calendar.getInstance();

                int currentDay = c.get(Calendar.DAY_OF_WEEK);
                int endDay = firstDayTable[profile.getDayStart()];

                Date startDate, endDate;

                if (endDay == currentDay) {
                    startDate = c.getTime();
                    c.add(Calendar.DAY_OF_YEAR, 6);
                    endDate = c.getTime();
                } else {
                    while (currentDay != endDay) {
                        c.add(Calendar.DATE, 1);
                        currentDay = c.get(Calendar.DAY_OF_WEEK);
                    }
                    c.add(Calendar.DAY_OF_YEAR, -1);
                    endDate = c.getTime();
                    c.add(Calendar.DAY_OF_YEAR, -6);
                    startDate = c.getTime();
                }

                //if it's the same month , show the month name only once
                if (startDate.getMonth() == endDate.getMonth()) {
                    tvBarDate.setText(getString(R.string.text_pie_heading) + "\n("
                            + new SimpleDateFormat("dd").format(startDate) + "-" +
                            new SimpleDateFormat("dd MMMM").format(endDate) + ")");
                } else {
                    tvBarDate.setText(getString(R.string.text_pie_heading) + "\n("
                            + new SimpleDateFormat("dd MMMM").format(startDate) + "-" +
                            new SimpleDateFormat("dd MMMM").format(endDate) + ")");
                }
                //else if it's monthly
            } else if (manager.getPrefsGrouping().equalsIgnoreCase(getResources().getString(R.string.pref_grouping_monthly))) {

                Calendar c = Calendar.getInstance();
                //if first day of Month is 1 , then show month name
                if (profile.getDayStart() == 0) {

                    int month = c.get(Calendar.MONTH);

                    //get month name
                    String sMonth = getMonthForInt(month);

                    tvBarDate.setText(sMonth);
                    //else show the period
                } else {

                    int monthStart = firstDayTable[profile.getDayStart()];

                    Date startDate, endDate;

                    c.set(Calendar.DAY_OF_MONTH, monthStart);
                    startDate = c.getTime();

                    c.add(Calendar.MONTH, 1);
                    c.add(Calendar.DAY_OF_MONTH, -1);
                    endDate = c.getTime();

                    tvBarDate.setText(getString(R.string.text_period) + "\n("
                            + new SimpleDateFormat("dd MMMM").format(startDate) + "-" +
                            new SimpleDateFormat("dd MMMM").format(endDate) + ")");

                }
            } else if (manager.getPrefsGrouping().equalsIgnoreCase(getResources().getString(R.string.pref_grouping_daily))) {
                tvBarDate.setText(getString(R.string.text_today));
            } else {
                tvBarDate.setText(getString(R.string.text_total));
            }

            //show the bars after set up
            card_bars.setVisibility(View.VISIBLE);
            card_message.setVisibility(View.GONE);

        } else {

            //else the bars section should be invisible
            card_bars.setVisibility(View.GONE);
            card_message.setVisibility(View.VISIBLE);
        }

        //  Cursor cursorLastExpense, cursorLastIncome;
        cursorLastExpense = mdb.getExpensesFromNewestToOldest();
        cursorLastIncome = mdb.getIncomesByNewestToOldest();

        //if there isn't currently an expense
        if (cursorLastExpense.moveToFirst()) {
            card_last_transactions.setVisibility(View.VISIBLE);
            llLastExpense.setVisibility(View.VISIBLE);

            //get last expense's date
            String date = cursorLastExpense.getString(2);
            String tokens[] = date.split("-");
            date = tokens[2] + "-" + tokens[1] + "-" + tokens[0];

            try {
                Calendar today = Calendar.getInstance();
                Calendar yesterday = Calendar.getInstance();
                yesterday.add(Calendar.DAY_OF_YEAR, -1);
                Calendar item_calendar = Calendar.getInstance();
                SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");

                Date item_date = format.parse(date);
                item_calendar.setTime(item_date);

                boolean isToday = today.get(Calendar.YEAR) == item_calendar.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == item_calendar.get(Calendar.DAY_OF_YEAR);
                boolean isYesterday = yesterday.get(Calendar.YEAR) == item_calendar.get(Calendar.YEAR) &&
                        yesterday.get(Calendar.DAY_OF_YEAR) == item_calendar.get(Calendar.DAY_OF_YEAR);
                if (isToday) {
                    tvLastExpenseDate.setText(getString(R.string.text_today));
                } else if (isYesterday) {
                    tvLastExpenseDate.setText(getString(R.string.text_yesterday));
                } else {
                    tvLastExpenseDate.setText(new SimpleDateFormat("dd MMMM").format(item_date));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //and fill the views with the values
            if (cursorLastExpense.getDouble(3) == (int) cursorLastExpense.getDouble(3)) {
                tvLastExpenseValue.setText((int) cursorLastExpense.getDouble(3) + " " + profile.getCurrency());
            } else {
                tvLastExpenseValue.setText(cursorLastExpense.getDouble(3) + " " + profile.getCurrency());
            }

            //open category database
            CategoryDatabase cdb = new CategoryDatabase(this);

            //and get the right color and letter according to the last expense category
            int color = cdb.getColorFromCategory(cursorLastExpense.getString(1), true);
            char letter = cdb.getLetterFromCategory(cursorLastExpense.getString(1), true);
            cdb.close();

            //set the letter image views accordingly
            livLastExpense.setLetter(letter);
            livLastExpense.setmBackgroundPaint(color);


        } else {
            //else disappear the last expense module
            llLastExpense.setVisibility(View.GONE);
        }
        if (cursorLastIncome.moveToFirst()) {
            card_last_transactions.setVisibility(View.VISIBLE);
            llLastIncome.setVisibility(View.VISIBLE);

            //get last income's date
            String date = cursorLastIncome.getString(3);
            String tokens[] = date.split("-");
            date = tokens[2] + "-" + tokens[1] + "-" + tokens[0];

            try {
                Calendar today = Calendar.getInstance();
                Calendar yesterday = Calendar.getInstance();
                yesterday.add(Calendar.DAY_OF_YEAR, -1);
                Calendar item_calendar = Calendar.getInstance();
                SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");

                Date item_date = format.parse(date);
                item_calendar.setTime(item_date);


                boolean isToday = today.get(Calendar.YEAR) == item_calendar.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == item_calendar.get(Calendar.DAY_OF_YEAR);
                boolean isYesterday = yesterday.get(Calendar.YEAR) == item_calendar.get(Calendar.YEAR) &&
                        yesterday.get(Calendar.DAY_OF_YEAR) == item_calendar.get(Calendar.DAY_OF_YEAR);
                if (isToday) {
                    tvLastIncomeDate.setText(getString(R.string.text_today));
                } else if (isYesterday) {
                    tvLastIncomeDate.setText(getString(R.string.text_yesterday));
                } else {
                    tvLastIncomeDate.setText(new SimpleDateFormat("dd MMMM").format(item_date));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //and fill the views with the values
            if (cursorLastIncome.getDouble(1) == (int) cursorLastIncome.getDouble(1)) {
                tvLastIncomeValue.setText((int) cursorLastIncome.getDouble(1) + " " + profile.getCurrency());
            } else {
                tvLastIncomeValue.setText(cursorLastIncome.getDouble(1) + " " + profile.getCurrency());
            }

            //open category database
            CategoryDatabase cdb = new CategoryDatabase(this);

            //and get the right color and letter according to the last expense category
            int color = cdb.getColorFromCategory(cursorLastIncome.getString(2), false);
            char letter = cdb.getLetterFromCategory(cursorLastIncome.getString(2), false);

            livLastIncome.setLetter(letter);
            livLastIncome.setmBackgroundPaint(color);

            cdb.close();

        } else {
            //else disappear the last income module
            llLastIncome.setVisibility(View.GONE);
        }
        //if neither expense or income exists , disappear the last transactions section
        if (!cursorLastIncome.moveToFirst() && !cursorLastExpense.moveToFirst()) {
            card_last_transactions.setVisibility(View.GONE);
        }

    }


    private void initListeners() {

        llLastExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cursorLastExpense.moveToFirst();
                Intent processExpense = new Intent(OverviewActivity.this, AddExpenseActivity.class);
                ExpenseItem expense = new ExpenseItem(cursorLastExpense.getString(1), cursorLastExpense.getString(4), Double.parseDouble(cursorLastExpense.getString(3)), cursorLastExpense.getString(2));
                expense.setId(Integer.parseInt(cursorLastExpense.getString(0)));
                processExpense.putExtra("Expense", expense);
                startActivity(processExpense);
            }
        });

        llLastIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cursorLastIncome.moveToFirst();
                Intent processIncome = new Intent(OverviewActivity.this, AddIncomeActivity.class);
                IncomeItem income = new IncomeItem(Double.parseDouble(cursorLastIncome.getString(1)), cursorLastIncome.getString(3), cursorLastIncome.getString(2), cursorLastIncome.getString(4));
                income.setId(Integer.parseInt(cursorLastIncome.getString(0)));
                processIncome.putExtra("Income", income);
                startActivity(processIncome);
            }
        });


    }


    private ListView.OnItemClickListener drawerClickListener = new ListView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case 0:
                    startActivity(new Intent(OverviewActivity.this, HistoryActivity.class));
                    break;
                case 1:
                    startActivity(new Intent(OverviewActivity.this, RecurrentTransactionsActivity.class));
                    break;
                case 2:
                    startActivity(new Intent(OverviewActivity.this, AddIncomeActivity.class));
                    break;
                case 3:
                    startActivity(new Intent(OverviewActivity.this, AddExpenseActivity.class));
                    break;
                case 4:
                    startActivity(new Intent(OverviewActivity.this, PieDistributionActivity.class));
                    break;
                case 5:
                    startActivity(new Intent(OverviewActivity.this, BarsDistributionActivity.class));
                    break;
                case 6:
                    startActivity(new Intent(OverviewActivity.this, CategoriesManagerActivity.class));
                    break;
                case 7:
                    startActivity(new Intent(OverviewActivity.this, SettingsActivity.class));
                    break;
                case 8:
                    finish();
                    break;
            }
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_overview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_addExpense) {
            startActivity(new Intent(getApplicationContext(), AddExpenseActivity.class));
        }
        if (id == R.id.action_addIncome) {
            startActivity(new Intent(getApplicationContext(), AddIncomeActivity.class));
        }
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //if drawer is open , close it
        if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            drawerLayout.closeDrawer(Gravity.LEFT);
        }
        //else , default action
        else {
            super.onBackPressed();
        }
    }
}
