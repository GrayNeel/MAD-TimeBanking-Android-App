package it.polito.mad.g01_timebanking.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.PropertyName
import it.polito.mad.g01_timebanking.R
import it.polito.mad.g01_timebanking.ui.SkillDiffCallback
import it.polito.mad.g01_timebanking.ui.timeslotlistbyskill.TimeSlotListBySkillViewModel
import java.util.*
import kotlin.collections.ArrayList

data class SkillDetails (
    var name: String = "",
    @set:PropertyName("usage_in_adv")
    @get:PropertyName("usage_in_adv")
    var usageInAdv: Long = 0L,
    @set:PropertyName("usage_in_user")
    @get:PropertyName("usage_in_user")
    var usageInUser: Long = 0L
){
    override fun equals(other: Any?): Boolean {
        other as SkillDetails
        return name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + usageInAdv.hashCode()
        result = 31 * result + usageInUser.hashCode()
        return result
    }

}

class SkillAdapter(
    private var data:List<SkillDetails>,
    private val tsListBySkillViewModel: TimeSlotListBySkillViewModel)
    : RecyclerView.Adapter<SkillAdapter.SkillViewHolder>(), Filterable {

    var dataFull = data.toList()
    val dataMutable = data.toMutableList()

    class SkillViewHolder(v:View): RecyclerView.ViewHolder(v) {
        private val title: TextView = v.findViewById(R.id.singleSkillTitle)
        private val usage: TextView = v.findViewById(R.id.singleSkillUsage)
        private val cardView: ImageButton = v.findViewById(R.id.skillButton)

        fun bind(skill: SkillDetails, cardAction: (v: View) -> Unit) {
            val skillUpCase = skill.name[0].uppercase() + skill.name.substring(1,skill.name.length)
            title.text = skillUpCase
            usage.text = skill.usageInAdv.toString()
            cardView.setOnClickListener(cardAction)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        // Get a reference to the context of recyclerview (current activity)
        val v : View = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.single_skill_layout, parent,false)
        return SkillViewHolder(v)
    }

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        val skill = data[position]
        val action = R.id.action_nav_skills_list_to_timeSlotListBySkillFragment

        // This callback will be called when the cardCallback of the list is clicked
        holder.bind(skill) {
            val pos = data.indexOf(skill)
            if (pos != -1) {
                tsListBySkillViewModel.setAdvertisementsBySkill(skill)
                tsListBySkillViewModel.removeFilters()
                Navigation.findNavController(it).navigate(action)
            }
        }

    }

    fun setSkills(newSkills: List<SkillDetails>) {
        val diffs = DiffUtil.calculateDiff( SkillDiffCallback(data, newSkills) )
        data = newSkills.toList() //update data
        diffs.dispatchUpdatesTo(this) //animate UI
    }


    override fun getItemCount(): Int = data.size


    override fun getFilter(): Filter {
        return object : Filter() {
            /*            override fun convertResultToString(resultValue: Any) :String {
                            Log.d("AutoComplete_Adapter", "Result value: $resultValue")
                            return (resultValue as SkillDetails).name
                        }*/
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                Log.d("AutoComplete_Adapter", "Constraint is $constraint")
                val filterResults = FilterResults()
                if (constraint != null) {
                    val skillSuggestion: MutableList<SkillDetails> = ArrayList()
                    Log.d("AutoComplete_Adapter", "Filtering with dataFull: $dataFull")
                    for (skill in dataFull) {
                        if (skill.name.lowercase(Locale.getDefault())
                                .startsWith(constraint.toString().lowercase(Locale.getDefault()).trim())
                        ) {
                            skillSuggestion.add(skill)
                        }
                    }
                    filterResults.values = skillSuggestion
                    filterResults.count = skillSuggestion.size
                }
                Log.d("AutoComplete_Adapter", "Filter Result count: ${filterResults.count}")
                Log.d("AutoComplete_Adapter", "Filter Result value: ${filterResults.values}")
                return filterResults
            }

            //Method clear, add, addAll are called on skillList.
            //This happen because we pass skillList to the super class constructor ArrayAdapter
            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults
            ) {
                dataMutable.clear()
                if (results.count > 0) {
                    for (result in results.values as List<*>) {
                        if (result is SkillDetails) {
                            dataMutable.add(result)
                        }
                    }
                    setSkills(dataMutable)
                }
                else if (results.count == 0){
                    dataMutable.clear()
                    setSkills(dataMutable)
                }
                else if (constraint == null) {
                    dataMutable.addAll(dataFull)
                    setSkills(dataMutable)
                }
            }
        }
    }
}



/*
*   Adapter used in AutoCompleteTextView to suggest skills
*
* */

class AutoCompleteSkillAdapter(context: Context, skillList: List<SkillDetails>) :
    ArrayAdapter<SkillDetails?>(context, 0, skillList.toMutableList() as List<SkillDetails?>) {
//The collection passed to the super ArrayAdapter is the one used by getItem, getSize, .clear, .addAll
    //different from Java, we have to pass it a mutableList to make this working

    private val skillListFull: List<SkillDetails> = ArrayList(skillList)
    private var skillList: MutableList<SkillDetails> = ArrayList(skillList)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        Log.d("AutoComplete_Adapter", "Actual skillList: $skillList")

        var internalConvertView = convertView
        if (internalConvertView == null) {
            internalConvertView = LayoutInflater.from(context).inflate(
                R.layout.single_skill_autocomplete_row, parent, false
            )
        }
        val textViewName = internalConvertView!!.findViewById<TextView>(R.id.skill_suggestion_name)
        val textViewUsage = internalConvertView.findViewById<TextView>(R.id.skill_suggestion_usage)
        //val imageViewSkill = internalConvertView!!.findViewById<ImageView>(R.id.image_view_skill)
        val skillItem: SkillDetails? = getItem(position)
        if(skillItem!= null) {
            textViewName.text = skillItem.name
            textViewUsage.text = skillItem.usageInAdv.toString()
        }
        //imageViewSkill.setImageResource(skillItem.)
        return internalConvertView
    }



    override fun getFilter(): Filter {
        return object : Filter() {
/*            override fun convertResultToString(resultValue: Any) :String {
                Log.d("AutoComplete_Adapter", "Result value: $resultValue")
                return (resultValue as SkillDetails).name
            }*/
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                Log.d("AutoComplete_Adapter", "Constraint is $constraint")
                val filterResults = FilterResults()
                if (constraint != null) {
                    val skillSuggestion: MutableList<SkillDetails> = ArrayList()
                    for (skill in skillListFull) {
                        if (skill.name.lowercase(Locale.getDefault())
                                .startsWith(constraint.toString().lowercase(Locale.getDefault()).trim())
                        ) {
                            skillSuggestion.add(skill)
                        }
                    }
                    filterResults.values = skillSuggestion
                    filterResults.count = skillSuggestion.size
                }
                Log.d("AutoComplete_Adapter", "Filter Result count: ${filterResults.count}")
                Log.d("AutoComplete_Adapter", "Filter Result value: ${filterResults.values}")
                return filterResults
            }

            //Method clear, add, addAll are called on skillList.
            //This happen because we pass skillList to the super class constructor ArrayAdapter
            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults
            ) {
                clear()
                if (results.count > 0) {
                    for (result in results.values as List<*>) {
                        if (result is SkillDetails) {
                            add(result)
                        }
                    }
                    notifyDataSetChanged()
                } else if (constraint == null) {
                    addAll(skillListFull)
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}