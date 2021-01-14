package app.multicontactpicker;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.l4digital.fastscroll.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.multicontactpicker.RxContacts.Contact;
import app.multicontactpicker.RxContacts.RxContacts;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class MultiContactPickerActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT_SELECTION = "extra_result_selection";
    private FastScrollRecyclerView recyclerView;
    private final List<Contact> contactList = new ArrayList<>();
    private MaterialButton btnSelectAll;
    private MaterialButton btnFinish;
    private TextView tvNoContacts;
    private LinearLayout controlPanel;
    private MultiContactPickerAdapter adapter;
    private androidx.appcompat.widget.Toolbar toolbar;
    private ProgressBar progressBar;
    private MultiContactPicker.Builder builder;
    private boolean allSelected = false;
    private CompositeDisposable disposables;
    private Integer animationCloseEnter, animationCloseExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) return;

        builder = (MultiContactPicker.Builder) intent.getSerializableExtra("builder");

        disposables = new CompositeDisposable();

        assert builder != null;
        setTheme(builder.theme);

        setContentView(R.layout.activity_multi_contact_picker);

        toolbar = findViewById(R.id.toolbar);
        controlPanel = findViewById(R.id.controlPanel);
        progressBar = findViewById(R.id.progressBar);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnFinish = findViewById(R.id.btnFinish);
        tvNoContacts = findViewById(R.id.tvNoContacts);
        recyclerView = findViewById(R.id.recyclerView);

        initialiseUI(builder);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MultiContactPickerAdapter(contactList, new MultiContactPickerAdapter.ContactSelectListener() {
            @Override
            public void onContactSelected(Contact contact, int totalSelectedContacts) {
                btnFinish.setEnabled(totalSelectedContacts > 0);
                if (builder.selectionMode == MultiContactPicker.CHOICE_MODE_SINGLE) {
                    finishPicking();
                }
            }
        });

        loadContacts();

        recyclerView.setAdapter(adapter);

        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishPicking();
            }
        });

        btnSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allSelected = !allSelected;
                if (adapter != null)
                    adapter.setAllSelected(allSelected);
                if (allSelected) {
                    btnSelectAll.setText(getString(R.string.tv_clear_all_btn_text));
                } else {
                    btnSelectAll.setText(getString(R.string.tv_select_all_btn_text));
                }
            }
        });

    }

    private void finishPicking() {
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_SELECTION, MultiContactPicker.buildResult(adapter.getSelectedContacts()));
        setResult(RESULT_OK, result);
        finish();
        overrideAnimation();
    }


    private void overrideAnimation() {
        if (animationCloseEnter != null && animationCloseExit != null) {
            overridePendingTransition(animationCloseEnter, animationCloseExit);
        }
    }

    private void initialiseUI(MultiContactPicker.Builder builder) {
        setSupportActionBar(toolbar);

        this.animationCloseEnter = builder.animationCloseEnter;
        this.animationCloseExit = builder.animationCloseExit;

        int color = ContextCompat.getColor(getApplicationContext(), R.color.selection_bg_color);
        recyclerView.setHandleColor(color);
        recyclerView.setBubbleColor(color);

        if (builder.bubbleColor != 0)
            recyclerView.setBubbleColor(builder.bubbleColor);
        if (builder.handleColor != 0)
            recyclerView.setHandleColor(builder.handleColor);
        if (builder.bubbleTextColor != 0)
            recyclerView.setBubbleTextColor(builder.bubbleTextColor);
        if (builder.trackColor != 0)
            recyclerView.setTrackColor(builder.trackColor);
        recyclerView.setHideScrollbar(builder.hideScrollbar);
        recyclerView.setTrackVisible(builder.showTrack);
        if (builder.selectionMode == MultiContactPicker.CHOICE_MODE_SINGLE) {
            controlPanel.setVisibility(View.GONE);
        } else {
            controlPanel.setVisibility(View.VISIBLE);
        }

        if (builder.selectionMode == MultiContactPicker.CHOICE_MODE_SINGLE && builder.selectedItems.size() > 0) {
            throw new RuntimeException("You must be using MultiContactPicker.CHOICE_MODE_MULTIPLE in order to use setSelectedContacts()");
        }

        if (builder.titleText != null) {
            setTitle(builder.titleText);
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            overrideAnimation();
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadContacts() {
        btnSelectAll.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        RxContacts.fetch(builder.columnLimit, this)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) {
                        disposables.add(disposable);
                    }
                })
                .filter(new Predicate<Contact>() {
                    @Override
                    public boolean test(@NonNull Contact contact) {
                        return contact.getDisplayName() != null;
                    }
                })
                .subscribe(new Observer<Contact>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull Contact value) {
                        contactList.add(value);
                        if (builder.selectedItems.contains(value.getId())) {
                            adapter.setContactSelected(value.getId());
                        }
                        Collections.sort(contactList, new Comparator<Contact>() {
                            @Override
                            public int compare(Contact contact, Contact t1) {
                                return contact.getDisplayName().compareToIgnoreCase(t1.getDisplayName());
                            }
                        });
                        if (builder.loadingMode == MultiContactPicker.LOAD_ASYNC) {
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        if (contactList.size() == 0) {
                            tvNoContacts.setVisibility(View.VISIBLE);
                        }
                        if (adapter != null && builder.loadingMode == MultiContactPicker.LOAD_SYNC) {
                            adapter.notifyDataSetChanged();
                        }

                        if (adapter != null) {
                            btnFinish.setEnabled((adapter.getSelectedContactsCount() > 0));
                        }
                        progressBar.setVisibility(View.GONE);
                        btnSelectAll.setEnabled(true);
                    }
                });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mcp_menu_main, menu);

        MenuItem searchMenuItem = menu.findItem(R.id.mcp_action_search);
        setSearchIconColor(searchMenuItem, builder.searchIconColor);

        SearchView searchView = (androidx.appcompat.widget.SearchView) searchMenuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (adapter != null) {
                    adapter.filterOnText(query);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.filterOnText(newText);
                }
                return false;
            }
        });
        return true;
    }

    private void setSearchIconColor(MenuItem menuItem, final Integer color) {
        if (color != null) {
            Drawable drawable = menuItem.getIcon();
            if (drawable != null) {
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTint(drawable.mutate(), color);
                menuItem.setIcon(drawable);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overrideAnimation();
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }
}
