package net.whollynugatory.android.comiccollector;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import net.whollynugatory.android.comiccollector.SignInActivity;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SignInActivityTest {

  @Rule
  public ActivityTestRule<SignInActivity> mActivityTestRule = new ActivityTestRule<>(SignInActivity.class);

  @Test
  public void signInActivityTest() {
    ViewInteraction frameLayout = onView(
      allOf(withId(R.id.sign_in_button_google),
        childAtPosition(
          allOf(withId(R.id.activity_sign_in),
            childAtPosition(
              withId(android.R.id.content),
              0)),
          1),
        isDisplayed()));
    frameLayout.check(matches(isDisplayed()));

    ViewInteraction pu = onView(
      allOf(withText("Sign in"),
        childAtPosition(
          allOf(withId(R.id.sign_in_button_google),
            childAtPosition(
              withId(R.id.activity_sign_in),
              2)),
          0),
        isDisplayed()));
    pu.perform(click());

//    ViewInteraction linearLayout = onView(
//      allOf(withId(com.google.android.gms.R.id.account_picker),
//        childAtPosition(
//          allOf(withId(android.R.id.content),
//            childAtPosition(
//              withId(com.google.android.gms.R.id.action_bar_root),
//              0)),
//          0),
//        isDisplayed()));
//    linearLayout.check(matches(isDisplayed()));
//
//    ViewInteraction textView = onView(
//      allOf(withId(com.google.android.gms.R.id.account_display_name), withText("Scrambled N"),
//        childAtPosition(
//          childAtPosition(
//            IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
//            1),
//          0),
//        isDisplayed()));
//    textView.check(matches(withText("Scrambled N")));
//
//    ViewInteraction textView2 = onView(
//      allOf(withId(com.google.android.gms.R.id.account_name), withText("scrambled.neurons@gmail.com"),
//        childAtPosition(
//          childAtPosition(
//            IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
//            1),
//          1),
//        isDisplayed()));
//    textView2.check(matches(withText("scrambled.neurons@gmail.com")));
  }

  private static Matcher<View> childAtPosition(
    final Matcher<View> parentMatcher, final int position) {

    return new TypeSafeMatcher<View>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Child at position " + position + " in parent ");
        parentMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(View view) {
        ViewParent parent = view.getParent();
        return parent instanceof ViewGroup && parentMatcher.matches(parent)
          && view.equals(((ViewGroup) parent).getChildAt(position));
      }
    };
  }
}
