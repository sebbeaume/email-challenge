import org.example.OfficeHours
import org.example.User
import java.time.ZoneId

class UserTestFactory {

    companion object{
        fun createSingaporeUserWithName(name: String): User{
            return User(name, OfficeHours(ZoneId.of("Asia/Singapore"), 8, 17))
        }
    }

}