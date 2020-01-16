package net.whollynugatory.android.comiccollector;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;
import net.whollynugatory.android.comiccollector.SignInActivity;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AddComicActivityTest {

  // TODO: addNewComic
  // TODO: addDuplicateComic
  // TODO: addUnknownPublisher
  // TODO: addUnknownSeries

  @Rule
  public ActivityTestRule<SignInActivity> mActivityTestRule = new ActivityTestRule<>(SignInActivity.class);

  @Rule
  public GrantPermissionRule mGrantPermissionRule =
    GrantPermissionRule.grant(
      "android.permission.CAMERA",
      "android.permission.WRITE_EXTERNAL_STORAGE");

  @Test
  public void addComicActivityTest() {
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

    ViewInteraction imageButton = onView(
      allOf(withId(R.id.comic_fab_add),
        childAtPosition(
          childAtPosition(
            withId(R.id.main_fragment_container),
            0),
          2),
        isDisplayed()));
    imageButton.check(matches(isDisplayed()));

    ViewInteraction floatingActionButton = onView(
      allOf(withId(R.id.comic_fab_add),
        childAtPosition(
          childAtPosition(
            withId(R.id.main_fragment_container),
            0),
          2),
        isDisplayed()));
    floatingActionButton.perform(click());

//    ViewInteraction textView = onView(
//      allOf(withId(com.android.packageinstaller.R.id.permission_message),
//        withText("Allow Comic Collector to take pictures and record video?"),
//        childAtPosition(
//          allOf(withId(com.android.packageinstaller.R.id.perm_desc_root),
//            childAtPosition(
//              withId(com.android.packageinstaller.R.id.desc_container),
//              0)),
//          1),
//        isDisplayed()));
//    textView.check(matches(withText("Allow Comic Collector to take pictures and record video?")));
//
//    ViewInteraction button = onView(
//      allOf(withId(com.android.packageinstaller.R.id.permission_deny_button),
//        childAtPosition(
//          allOf(withId(com.android.packageinstaller.R.id.button_group),
//            childAtPosition(
//              IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
//              0)),
//          0),
//        isDisplayed()));
//    button.check(matches(isDisplayed()));
//
//    ViewInteraction button2 = onView(
//      allOf(withId(com.android.packageinstaller.R.id.permission_allow_button),
//        childAtPosition(
//          allOf(withId(com.android.packageinstaller.R.id.button_group),
//            childAtPosition(
//              IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
//              0)),
//          1),
//        isDisplayed()));
//    button2.check(matches(isDisplayed()));

    ViewInteraction appCompatButton = onView(
      allOf(withId(R.id.tutorial_button_continue), withText("Continue"),
        childAtPosition(
          childAtPosition(
            withId(R.id.add_fragment_container),
            0),
          4),
        isDisplayed()));
    appCompatButton.perform(click());

    // TODO: missing camera intent interception

    ViewInteraction editText = onView(
      allOf(withId(R.id.manual_search_edit_product), withText("759606084791"),
        childAtPosition(
          childAtPosition(
            withId(R.id.add_fragment_container),
            0),
          1),
        isDisplayed()));
    editText.check(matches(isDisplayed()));

    ViewInteraction button3 = onView(
      allOf(withId(R.id.manual_search_button_continue),
        childAtPosition(
          childAtPosition(
            withId(R.id.add_fragment_container),
            0),
          6),
        isDisplayed()));
    button3.check(matches(isDisplayed()));

    ViewInteraction appCompatEditText = onView(
      allOf(withId(R.id.manual_search_edit_issue),
        childAtPosition(
          childAtPosition(
            withId(R.id.add_fragment_container),
            0),
          5),
        isDisplayed()));
    appCompatEditText.perform(replaceText("00111"), closeSoftKeyboard());

    ViewInteraction appCompatButton2 = onView(
      allOf(withId(R.id.manual_search_button_continue), withText("Continue"),
        childAtPosition(
          childAtPosition(
            withId(R.id.add_fragment_container),
            0),
          7),
        isDisplayed()));
    appCompatButton2.perform(click());

    ViewInteraction toggleButton = onView(
      allOf(withId(R.id.comic_book_toggle_owned),
        childAtPosition(
          childAtPosition(
            withId(R.id.main_fragment_container),
            0),
          14),
        isDisplayed()));
    toggleButton.check(matches(isDisplayed()));

    ViewInteraction toggleButton2 = onView(
      allOf(withId(R.id.comic_book_toggle_read),
        childAtPosition(
          childAtPosition(
            withId(R.id.main_fragment_container),
            0),
          15),
        isDisplayed()));
    toggleButton2.check(matches(isDisplayed()));

    ViewInteraction button4 = onView(
      allOf(withId(R.id.comic_book_button_save),
        childAtPosition(
          childAtPosition(
            withId(R.id.main_fragment_container),
            0),
          16),
        isDisplayed()));
    button4.check(matches(isDisplayed()));

    ViewInteraction appCompatButton3 = onView(
      allOf(withId(R.id.comic_book_button_save), withText("Save"),
        childAtPosition(
          childAtPosition(
            withId(R.id.main_fragment_container),
            0),
          16),
        isDisplayed()));
    appCompatButton3.perform(click());

    ViewInteraction viewGroup = onView(
      allOf(childAtPosition(
        allOf(withId(R.id.comic_list_view), withContentDescription("List of comics."),
          childAtPosition(
            IsInstanceOf.instanceOf(android.view.ViewGroup.class),
            0)),
        0),
        isDisplayed()));
    viewGroup.check(matches(isDisplayed()));

    ViewInteraction textView2 = onView(
      allOf(withId(R.id.comic_item_text_series), withText("X-Men The Seeds of Tomorrow"),
        childAtPosition(
          childAtPosition(
            allOf(withId(R.id.comic_list_view), withContentDescription("List of comics.")),
            0),
          0),
        isDisplayed()));
    textView2.check(matches(withText("X-Men The Seeds of Tomorrow")));
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
