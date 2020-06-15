package cz.vsb.cbe.testbed.fragments;

import android.util.Log;

import androidx.fragment.app.Fragment;

import java.time.YearMonth;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.jar.Manifest;

public class BaseFragment extends Fragment {

    Date [] setPeriod() {
        Calendar startPeriod = Calendar.getInstance();
        startPeriod.add(Calendar.YEAR, 1900);
        startPeriod.add(Calendar.MONTH, 0);
        startPeriod.add(Calendar.DAY_OF_MONTH, 0);
        startPeriod.add(Calendar.HOUR_OF_DAY, 0);
        startPeriod.add(Calendar.MINUTE, 0);
        startPeriod.add(Calendar.SECOND, 0);
        startPeriod.add(Calendar.MILLISECOND, 0);

        Calendar endPeriod = Calendar.getInstance();
        endPeriod.add(Calendar.YEAR, 2100);
        endPeriod.add(Calendar.MONTH, 12);
        endPeriod.add(Calendar.DAY_OF_MONTH,startPeriod.getActualMaximum(Calendar.DAY_OF_MONTH));
        endPeriod.add(Calendar.HOUR_OF_DAY, 23);
        endPeriod.add(Calendar.MINUTE, 59);
        endPeriod.add(Calendar.SECOND, 59);
        endPeriod.add(Calendar.MILLISECOND, 999);

        return new Date[] {startPeriod.getTime(), endPeriod.getTime()};

    }

    Date [] setPeriod(int year) {

        Calendar startPeriod = new GregorianCalendar();
        startPeriod.set(year, 0, 1, 0, 0, 0);

        Calendar endPeriod = new GregorianCalendar();
        endPeriod.set(year, 12 -1 , 31, 23, 59, 59);

        return new Date[] {startPeriod.getTime(), endPeriod.getTime()};
    }

    Date [] setPeriod(int year, int month) {
        Calendar startPeriod = Calendar.getInstance();
        startPeriod.add(Calendar.YEAR, year);
        startPeriod.add(Calendar.MONTH, month);
        startPeriod.add(Calendar.DAY_OF_MONTH, 1);
        startPeriod.add(Calendar.HOUR_OF_DAY, 0);
        startPeriod.add(Calendar.MINUTE, 0);
        startPeriod.add(Calendar.SECOND, 0);
        startPeriod.add(Calendar.MILLISECOND, 0);

        Calendar endPeriod = Calendar.getInstance();
        endPeriod.add(Calendar.YEAR, year);
        endPeriod.add(Calendar.MONTH, month);
        endPeriod.add(Calendar.DAY_OF_MONTH,startPeriod.getActualMaximum(Calendar.DAY_OF_MONTH));
        endPeriod.add(Calendar.HOUR_OF_DAY, 23);
        endPeriod.add(Calendar.MINUTE, 59);
        endPeriod.add(Calendar.SECOND, 59);
        endPeriod.add(Calendar.MILLISECOND, 999);

        return new Date[] {startPeriod.getTime(), endPeriod.getTime()};

    }

    Date [] setPeriod(int year, int month, int dayOfMonth) {
        Calendar startPeriod = Calendar.getInstance();
        startPeriod.add(Calendar.YEAR, year);
        startPeriod.add(Calendar.MONTH, month);
        startPeriod.add(Calendar.DAY_OF_MONTH, dayOfMonth);
        startPeriod.add(Calendar.HOUR_OF_DAY, 0);
        startPeriod.add(Calendar.MINUTE, 0);
        startPeriod.add(Calendar.SECOND, 0);
        startPeriod.add(Calendar.MILLISECOND, 0);

        Calendar endPeriod = Calendar.getInstance();
        endPeriod.add(Calendar.YEAR, year);
        endPeriod.add(Calendar.MONTH, month);
        endPeriod.add(Calendar.DAY_OF_MONTH, dayOfMonth);
        endPeriod.add(Calendar.HOUR_OF_DAY, 23);
        endPeriod.add(Calendar.MINUTE, 59);
        endPeriod.add(Calendar.SECOND, 59);
        endPeriod.add(Calendar.MILLISECOND, 999);

        return new Date[] {startPeriod.getTime(), endPeriod.getTime()};

    }

    Date [] setPeriod(int year, int month, int dayOfMonth, int hour) {
        Calendar startPeriod = Calendar.getInstance();
        startPeriod.add(Calendar.YEAR, year);
        startPeriod.add(Calendar.MONTH, month);
        startPeriod.add(Calendar.DAY_OF_MONTH, dayOfMonth);
        startPeriod.add(Calendar.HOUR_OF_DAY, hour);
        startPeriod.add(Calendar.MINUTE, 0);
        startPeriod.add(Calendar.SECOND, 0);
        startPeriod.add(Calendar.MILLISECOND, 0);

        Calendar endPeriod = Calendar.getInstance();
        endPeriod.add(Calendar.YEAR, year);
        endPeriod.add(Calendar.MONTH, month);
        endPeriod.add(Calendar.DAY_OF_MONTH, dayOfMonth);
        endPeriod.add(Calendar.HOUR_OF_DAY, hour);
        endPeriod.add(Calendar.MINUTE, 59);
        endPeriod.add(Calendar.SECOND, 59);
        endPeriod.add(Calendar.MILLISECOND, 999);

        return new Date[] {startPeriod.getTime(), endPeriod.getTime()};

    }

    Date [] setPeriod(int year, int month, int dayOfMonth, int hour, int minute) {
        Calendar startPeriod = Calendar.getInstance();
        startPeriod.add(Calendar.YEAR, year);
        startPeriod.add(Calendar.MONTH, month);
        startPeriod.add(Calendar.DAY_OF_MONTH, dayOfMonth);
        startPeriod.add(Calendar.HOUR_OF_DAY, hour);
        startPeriod.add(Calendar.MINUTE, minute);
        startPeriod.add(Calendar.SECOND, 0);
        startPeriod.add(Calendar.MILLISECOND, 0);

        Calendar endPeriod = Calendar.getInstance();
        endPeriod.add(Calendar.YEAR, year);
        endPeriod.add(Calendar.MONTH, month);
        endPeriod.add(Calendar.DAY_OF_MONTH, dayOfMonth);
        endPeriod.add(Calendar.HOUR_OF_DAY, hour);
        endPeriod.add(Calendar.MINUTE, minute);
        endPeriod.add(Calendar.SECOND, 59);
        endPeriod.add(Calendar.MILLISECOND, 999);

        return new Date[] {startPeriod.getTime(), endPeriod.getTime()};

    }
}
