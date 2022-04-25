package it.polito.mad.g01_timebanking

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.polito.mad.g01_timebanking.UserKey.HASTOBEEMPTY
import it.polito.mad.g01_timebanking.adapters.AdvertisementDetails
import it.polito.mad.g01_timebanking.databinding.ActivityMainBinding
import it.polito.mad.g01_timebanking.ui.timeslotdetails.TimeSlotDetailsViewModel
import it.polito.mad.g01_timebanking.ui.timeslotlist.TimeSlotListFragment
import it.polito.mad.g01_timebanking.ui.timeslotlist.TimeSlotListViewModel
import java.lang.reflect.Type


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    /*
    class ExtraParamsViewModelFactory(
        private val application: Application,
        private val myExtraParam: String
    ): ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TimeSlotListViewModel(application, myExtraParam) as T
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // It creates the VM that will be used by fragments
        val vm : TimeSlotDetailsViewModel =
            ViewModelProvider(this)[TimeSlotDetailsViewModel::class.java]
        val advListViewModel : TimeSlotListViewModel =
            ViewModelProvider(this)[TimeSlotListViewModel::class.java]
        /*val advListViewModel: TimeSlotListViewModel by viewModels {
            ExtraParamsViewModelFactory(this.application, "some string value")
        }*/

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        fab.setOnClickListener {
            navController.navigate(
                R.id.action_nav_your_offers_to_nav_edit_time_slot,
                bundleOf(HASTOBEEMPTY to true)
            )
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
        }

        // This code hides the FAB when it is NOT showing the TimeSlotListFragment
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when ((destination as FragmentNavigator.Destination).className) {
                // show fab in recipe fragment
                TimeSlotListFragment::class.qualifiedName -> {
                    fab.visibility = View.VISIBLE
                }
                // hide on other fragments
                else -> {
                    fab.visibility = View.GONE
                }
            }

        }

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_show_time_slot, R.id.nav_your_offers
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        // Populate advertisements
        val typeMyType: Type = object : TypeToken<ArrayList<AdvertisementDetails?>?>() {}.type
        val gson = Gson()

        val sharedPref = this.getSharedPreferences(
            getString(R.string.preference_file_key), MODE_PRIVATE
        )
        val s: String = sharedPref.getString(getString(R.string.adv_list), "") ?: ""

        val l : MutableList<AdvertisementDetails> =
            if(s!="")
                gson.fromJson(s, typeMyType) as MutableList<AdvertisementDetails>
            else
                mutableListOf()
        println("Read: $s")
        advListViewModel.initializeAdvList(l)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}