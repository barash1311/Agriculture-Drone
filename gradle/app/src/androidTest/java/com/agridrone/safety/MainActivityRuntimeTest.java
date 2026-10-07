package com.agridrone.safety;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.agridrone.safety.ui.main.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class MainActivityRuntimeTest {

    @Rule
    public final ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void realTimeSearchCanStopAndSwitchToDemoScenario() {
        onView(withId(R.id.button_landing_real_time)).perform(click());
        onView(withText(R.string.mode_real_time_stop)).check(matches(isDisplayed()));
        onView(withId(R.id.layout_connection_landing)).check(matches(isDisplayed()));
        onView(withId(R.id.scroll_content)).check(matches(not(isDisplayed())));

        onView(withId(R.id.button_landing_real_time)).perform(click());
        onView(withText(R.string.landing_choose_mode)).check(matches(isDisplayed()));

        onView(withId(R.id.button_landing_demo)).perform(click());
        onView(withText(R.string.state_connected_demo)).check(matches(isDisplayed()));
        onView(withText(R.string.open_simulator_scenarios)).perform(click());
        onView(withText(R.string.scenario_notice)).perform(click());

        onView(withId(R.id.text_battery_safety_badge))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.safety_notice)));
        onView(withId(R.id.text_safety_banner_message))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.notice_message)));
    }
}
