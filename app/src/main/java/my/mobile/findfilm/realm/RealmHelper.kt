package my.mobile.findfilm.realm

import android.content.Context
import com.google.gson.Gson
import io.realm.Realm
import io.realm.RealmResults
import my.mobile.findfilm.data.Film
import my.mobile.findfilm.data.Television
import my.mobile.findfilm.data.model.User

class RealmHelper(context: Context) {
    private val realm: Realm
    private lateinit var films: RealmResults<Film>
    private lateinit var tvs: RealmResults<Television>

    init {
        Realm.init(context)
        realm = Realm.getDefaultInstance()
    }

    fun register(user: User){
        realm.beginTransaction()
        realm.copyToRealm(user)
        realm.commitTransaction()
    }
    fun isRegistred(user: User,): Boolean{
        val userRegistered = realm.where(User::class.java).equalTo(User::userId.name,user.userId).findFirst()
        return userRegistered?.isValid ?: false
    }
    fun isRegistred(username: String, password: String): Boolean{
        val userRegistered = realm.where(User::class.java).equalTo(User::username.name,username).equalTo(User::password.name,password).findFirst()
        return userRegistered?.isValid ?: false
    }
    fun isRegistred(username: String): Boolean{
        val userRegistered = realm.where(User::class.java).equalTo(User::username.name,username).findFirst()
        return userRegistered?.isValid ?: false
    }
    fun getUser(username: String, password: String): User? {
        return realm.where(User::class.java).equalTo(User::username.name, username)
            .equalTo(User::password.name, password).findFirst()
    }

    fun getAllFilmFav(): List<Film>{
        val data = mutableListOf<Film>()
        films = realm.where(Film::class.java).findAll()

        films.forEach{ film ->
            film?.apply {
                val filmDetach = realm.copyFromRealm(this)
                val json = Gson().toJson(filmDetach)
                data.add(Gson().fromJson(json,Film::class.java))
            }
        }
        return data
    }
    fun getAllTvFav(): List<Television>{
        val data = mutableListOf<Television>()
        tvs = realm.where(Television::class.java).findAll()

        tvs.forEach{ tv ->
            tv?.apply {
                val tvDetach = realm.copyFromRealm(this)
                val json = Gson().toJson(tvDetach)
                data.add(Gson().fromJson(json,Television::class.java))
            }
        }
        return data
    }
    fun addFilmFav(film: Film){
        realm.beginTransaction()
        realm.copyToRealmOrUpdate(film)
        realm.commitTransaction()
    }
    fun addTvFav(television: Television){
        realm.beginTransaction()
        realm.copyToRealmOrUpdate(television)
        realm.commitTransaction()
    }
    fun deleteFilmFav(film: Film){
        val flm: Film? = realm.where(Film::class.java).equalTo(Film::id.name,film.id).findFirst()
        realm.beginTransaction()
        flm?.deleteFromRealm()
        realm.commitTransaction()
    }
    fun deleteTvFav(television: Television){
        val tv: Television? = realm.where(Television::class.java).equalTo(Television::id.name,television.id).findFirst()
        realm.beginTransaction()
        tv?.deleteFromRealm()
        realm.commitTransaction()
    }
    fun isFilmFav(film: Film): Boolean = realm.where(Film::class.java).equalTo(Film::id.name,film.id).findFirst()?.isValid ?: false
    fun isTvFav(television: Television): Boolean = realm.where(Television::class.java).equalTo(Television::id.name,television.id).findFirst()?.isValid ?: false
}