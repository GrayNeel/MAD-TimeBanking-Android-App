package it.polito.mad.g01_timebanking.ui.timeslotdetails

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import it.polito.mad.g01_timebanking.UserInfo
import it.polito.mad.g01_timebanking.UserKey
import it.polito.mad.g01_timebanking.adapters.AdvertisementDetails
import it.polito.mad.g01_timebanking.adapters.SkillDetails
import kotlinx.coroutines.awaitAll
import java.util.*

class TimeSlotDetailsViewModel(a: Application) : AndroidViewModel(a) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private lateinit var suggestedSkillsListener: ListenerRegistration

    private val _adv = AdvertisementDetails(
        UserKey.ID_PLACEHOLDER,
        "Placeholder title",
        "Placeholder location",
        Calendar.getInstance().time,
        "Placeholder duration",
        "Placeholder description",
        "Placeholder uid"
    )


    /* This will be the valid advertisement used by Show */
    private val pvtAdvertisement = MutableLiveData<AdvertisementDetails>().also {
        it.value = _adv
    }

    val advertisement: LiveData<AdvertisementDetails> = pvtAdvertisement


    /* Ephemeral variables used from the Edit fragment to handle temporary save */
    private var tmpSkills: MutableSet<String> = _adv.skills.toMutableSet()

    private val pvtSkills = MutableLiveData<MutableSet<String>>().also {
        it.value = tmpSkills
    }
    val skills: LiveData<MutableSet<String>> = pvtSkills

    private var tmpSuggestedSkills: MutableSet<SkillDetails> =
        _adv.skills.map { SkillDetails(name = it) }.toMutableSet()

    private val pvtSuggestedSkills = MutableLiveData<MutableSet<SkillDetails>>().also {
        it.value = tmpSuggestedSkills
        getSuggestedSkills()
    }
    val suggestedSkills: LiveData<MutableSet<SkillDetails>> = pvtSuggestedSkills

    private val pvtId = MutableLiveData<String>().also {
        it.value = _adv.id
    }
    val id: LiveData<String> = pvtId

    private val pvtTitle = MutableLiveData<String>().also {
        it.value = _adv.title
    }
    val title: LiveData<String> = pvtTitle

    private val pvtDescription = MutableLiveData<String>().also {
        it.value = _adv.description
    }
    val description: LiveData<String> = pvtDescription

    private val pvtLocation = MutableLiveData<String>().also {
        it.value = _adv.location
    }
    val location: LiveData<String> = pvtLocation

    private val pvtCalendar = MutableLiveData<Calendar>().also {
        val calendar = Calendar.getInstance()
        calendar.time = _adv.calendar
        it.value = calendar
    }
    val calendar: LiveData<Calendar> = pvtCalendar

    private val pvtDuration = MutableLiveData<String>().also {
        it.value = _adv.duration
    }
    val duration: LiveData<String> = pvtDuration

    private val pvtSavedByList = MutableLiveData<MutableList<String>>().also {
        it.value = mutableListOf()
    }
    val savedByList: LiveData<MutableList<String>> = pvtSavedByList

    fun setTitle(title: String) {
        pvtTitle.value = title
    }

    fun setLocation(location: String) {
        pvtLocation.value = location
    }

    fun setDescription(description: String) {
        pvtDescription.value = description
    }

    fun setDuration(duration: String) {
        pvtDuration.value = duration
    }

    fun setDateTime(calendar: Calendar) {
        pvtCalendar.value = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendar.get(Calendar.YEAR))
            set(Calendar.MONTH, calendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
        }
    }

    private fun getSuggestedSkills() {

        suggestedSkillsListener = db.collection("suggestedSkills")
            .addSnapshotListener { value, error ->
                if (error == null && value != null) {
                    pvtSuggestedSkills.value = value.documents.map { it.toSkill() }.toMutableSet()
                }
            }
    }

    fun removeSkill(skillText: String) {
        tmpSkills.remove(skillText)
        pvtSkills.value = tmpSkills
    }

    fun tryToAddSkill(skillText: String): Boolean {

        return if (tmpSkills.add(skillText)) {
            pvtSkills.value = tmpSkills; true
        } else false
    }

    fun setAdvertisement(adv: AdvertisementDetails) {
        pvtAdvertisement.value = adv
        pvtId.value = adv.id
        pvtTitle.value = adv.title
        pvtLocation.value = adv.location
        pvtDescription.value = adv.description
        pvtDuration.value = adv.duration
        val calendar = Calendar.getInstance()
        calendar.time = adv.calendar
        pvtCalendar.value = calendar
        pvtSkills.value = adv.skills.toMutableSet()
        tmpSkills = adv.skills.toMutableSet()
        pvtSavedByList.value = adv.savedBy.toMutableList()
    }

    fun addToSavedList(actual: MutableList<String>){
        db.collection("advertisements")
            .document(pvtId.value!!)
            .update("savedBy",FieldValue.arrayUnion(auth.currentUser!!.uid) ).addOnSuccessListener {
                pvtSavedByList.value = actual
            }
    }
    fun removeFromSavedList(actual: MutableList<String>){
        db.collection("advertisements")
            .document(pvtId.value!!)
            .update("savedBy",FieldValue.arrayRemove(auth.currentUser!!.uid) ).addOnSuccessListener {
                pvtSavedByList.value = actual
            }

    }

    fun addOrUpdateSkills(newAdvSkillsName: MutableList<String>) {
        val oldSkills = pvtAdvertisement.value!!.skills

        val addedSkills = newAdvSkillsName.toSet() subtract oldSkills
        val removedSkills = oldSkills subtract newAdvSkillsName.toSet()
        increaseUsageOfSkills(addedSkills)
        decreaseUsageOfSkills(removedSkills)
    }

    fun decreaseUsageOfSkills(removedSkills: Set<String>) {
        removedSkills.forEach { removedSkill ->
            db.collection("suggestedSkills").document(removedSkill)
                .update(
                    "usage_in_adv",
                    FieldValue.increment(-1L)
                ).addOnSuccessListener {
                    Log.d("AdvSkill", "skill $removedSkill decremented")
                }
        }
    }

     fun increaseUsageOfSkills(addedSkills: Set<String>) {
        addedSkills.forEach { addedSkill ->
            db.collection("suggestedSkills").document(addedSkill)
                .set(
                    hashMapOf(
                        "name" to addedSkill,
                        "usage_in_adv" to FieldValue.increment(1L),
                        "usage_in_user" to FieldValue.increment(0L)
                    ),
                    SetOptions.merge()
                )
        }
    }

    fun prepareNewAdvertisement() {
        val expTime = Calendar.getInstance()
        expTime.add(Calendar.HOUR_OF_DAY, +2)
        expTime.set(Calendar.MINUTE, 0)

        pvtId.value = UserKey.ID_PLACEHOLDER
        pvtTitle.value = ""
        pvtLocation.value = ""
        pvtDescription.value = ""
        pvtDuration.value = ""
        pvtCalendar.value = expTime
        pvtSkills.value = mutableSetOf()
        tmpSkills = mutableSetOf()
        pvtSavedByList.value = mutableListOf()
    }

    fun clearTmpSkills(){
        tmpSkills = mutableSetOf()
    }

    fun getUserInfoFromDb(uid: String): DocumentReference {
        return db.collection("users").document(uid)
    }
}

private fun DocumentSnapshot.toSkill(): SkillDetails {
    return this.toObject(SkillDetails::class.java) ?: SkillDetails()
}