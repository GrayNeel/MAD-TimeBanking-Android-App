package it.polito.mad.g01_timebanking.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.ktx.Firebase
import it.polito.mad.g01_timebanking.UserInfo
import it.polito.mad.g01_timebanking.repositories.FirebaseRepository
import it.polito.mad.g01_timebanking.repositories.PreferencesRepository

class ProfileViewModel(a: Application): AndroidViewModel(a) {
    private val repo = PreferencesRepository(a)
    private val repo2 = FirebaseRepository(a,this)

    // Official variable that contains UserInfo saved on preferences
    private var _user = UserInfo()
    private val pvtUser = MutableLiveData<UserInfo>().also {
        it.value = _user
        repo2.getUserInfo()
    }
    val user : LiveData<UserInfo> = pvtUser

    /* Ephemeral variables used from the Edit fragment to handle temporary save */

    private val pvtFullName = MutableLiveData<String>().also {
        it.value = _user.fullName
    }
    val fullName : LiveData<String> = pvtFullName

    private val pvtNickname = MutableLiveData<String>().also {
        it.value = _user.nickname
    }
    val nickname : LiveData<String> = pvtNickname

    private val pvtEmail = MutableLiveData<String>().also {
        it.value = _user.email
    }
    val email : LiveData<String> = pvtEmail

    private val pvtBiography = MutableLiveData<String>().also {
        it.value = _user.biography
    }
    val biography : LiveData<String> = pvtBiography

    private val pvtLocation = MutableLiveData<String>().also {
        it.value = _user.location
    }
    val location : LiveData<String> = pvtLocation

    private val pvtProfilePicturePath = MutableLiveData<String>().also {
        it.value = _user.profilePicturePath
    }
    val profilePicturePath : LiveData<String> = pvtProfilePicturePath

    private var tmpSkills : MutableSet<String> = _user.skills

    private val pvtSkills = MutableLiveData<MutableSet<String>>().also {
        it.value = tmpSkills
    }
    val skills :LiveData<MutableSet<String>> = pvtSkills


    fun setFullname(fullname: String) {
        pvtFullName.value = fullname
    }

    fun setNickname(nickname: String) {
        pvtNickname.value = nickname
    }

    fun setEmail(email: String) {
        pvtEmail.value = email
    }

    fun setLocation(location: String) {
        pvtLocation.value = location
    }

    fun setBiography(biography: String) {
        pvtBiography.value = biography
    }

    fun setProfilePicturePath(profilePicturePath: String) {
        pvtProfilePicturePath.value = profilePicturePath
    }

    fun removeSkill(skillText: String) {
        tmpSkills.remove(skillText)
        pvtSkills.value = tmpSkills
    }

    fun tryToAddSkill(skillText: String): Boolean {
        return if(tmpSkills.add(skillText)){ pvtSkills.value = tmpSkills; true } else false
    }

    fun setUserInfo(userInfo: UserInfo) {
        _user = userInfo
        pvtUser.value = userInfo
        pvtFullName.value = userInfo.fullName
        pvtNickname.value = userInfo.nickname
        pvtEmail.value = userInfo.email
        pvtLocation.value = userInfo.location
        pvtBiography.value = userInfo.biography
        pvtProfilePicturePath.value = userInfo.profilePicturePath
        pvtSkills.value = userInfo.skills
    }

    fun addOrUpdateData(user: UserInfo) {
        repo2.insertOrUpdateUserInfo(user)
        pvtUser.value = user
        _user = user
    }

    fun updatePhoto(newProfilePicturePath: String) {
        val u = UserInfo (
            fullName = _user.fullName,
            nickname = _user.nickname,
            email = _user.email,
            location = _user.location,
            biography = _user.biography,
            profilePicturePath = newProfilePicturePath,
            skills = _user.skills
        )
        addOrUpdateData(u)
    }
}