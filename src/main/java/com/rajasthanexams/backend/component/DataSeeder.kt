package com.rajasthanexams.backend.component

import com.rajasthanexams.backend.model.Exam
import com.rajasthanexams.backend.model.Test
import com.rajasthanexams.backend.model.TestType
import com.rajasthanexams.backend.repository.ExamRepository
import com.rajasthanexams.backend.repository.TestRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class DataSeeder(
    private val examRepository: ExamRepository,
    private val testRepository: TestRepository,
    private val communityPostRepository: com.rajasthanexams.backend.repository.CommunityPostRepository,
    private val userRepository: com.rajasthanexams.backend.repository.UserRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        if (examRepository.count() == 0L) {
             // Create LDC Exam (Premium)
             val ldcExam = Exam(
                 title = "LDC",
                 category = "LDC",
                 isPremium = true,
                 price = 99.0,
                 iconUrl = "", 
                 marksPerQuestion = 1.0,
                 negativeMarks = 0.33
             )
             val savedLdc = examRepository.save(ldcExam)
             
             // Create a Test for LDC
             val ldcTest = Test(
                 exam = savedLdc,
                 title = "LDC Mock Test 1",
                 type = TestType.MOCK,
                 durationMinutes = 60,
                 totalQuestions = 100,
                 isPremium = true,
                 isLive = false,
                 startsAt = LocalDateTime.now(),
                 endsAt = LocalDateTime.now().plusYears(1),
                 marksPerQuestionOverride = 1.0,
                 totalMarks = 100.0
             )
             testRepository.save(ldcTest)

             // Create Computer Anudesk (Free)
             val caExam = Exam(
                 title = "Computer Anudesk",
                 category = "Instructor",
                 isPremium = false,
                 price = 0.0,
                 marksPerQuestion = 1.0,
                 negativeMarks = 0.33
             )
             val savedCa = examRepository.save(caExam)
             
              val caTest = Test(
                 exam = savedCa,
                 title = "Basic Computer Test",
                 type = TestType.MOCK,
                 durationMinutes = 30,
                 totalQuestions = 50,
                 isPremium = false,
                 isLive = false,
                 startsAt = LocalDateTime.now(),
                 endsAt = LocalDateTime.now().plusYears(1),
                 marksPerQuestionOverride = 1.0,
                 totalMarks = 50.0
             )
             testRepository.save(caTest)
             
             println("DataSeeder: Seeded initial exams and tests.")
        } else {
            // Check if LDC exists and has valid price
            val ldcExam = examRepository.findByTitle("LDC")
            if (ldcExam != null) {
                if (ldcExam.price == null || ldcExam.price == 0.0) {
                    val updatedLdc = ldcExam.copy(
                        price = 99.0,
                        isPremium = true
                    )
                    examRepository.save(updatedLdc)
                    println("DataSeeder: Updated LDC exam price to 99.0")
                }
            }
        }
        
        // Seed Community Posts
        if (communityPostRepository.count() == 0L) {
            // Ensure users exist
            val user1 = getOrCreateUser("9876543210", "Rahul Sharma")
            val user2 = getOrCreateUser("9876543211", "Priya Verma")
            val user3 = getOrCreateUser("9876543212", "Amit Singh")

            val posts = listOf(
                com.rajasthanexams.backend.model.CommunityPost(
                    userId = user1.id!!,
                    userName = user1.name ?: "Rahul Sharma",
                    userProfilePicture = "https://ui-avatars.com/api/?name=Rahul+Sharma",
                    content = "महाराणा प्रताप के घोड़े 'चेतक' की समाधि कहां स्थित है?",
                    subject = "History",
                    category = "Culture",
                    upvotes = 45,
                    commentCount = 12,
                    verifiedAnswer = "बलीचा गांव, राजसमंद में स्थित है।"
                ),
                com.rajasthanexams.backend.model.CommunityPost(
                    userId = user2.id!!,
                    userName = user2.name ?: "Priya Verma",
                    userProfilePicture = "https://ui-avatars.com/api/?name=Priya+Verma",
                    content = "What is the correct order of Aravalli peaks by height?",
                    subject = "Geography",
                    category = "Geography",
                    upvotes = 32,
                    commentCount = 8
                ),
                com.rajasthanexams.backend.model.CommunityPost(
                    userId = user3.id!!,
                    userName = user3.name ?: "Amit Singh",
                    userProfilePicture = "https://ui-avatars.com/api/?name=Amit+Singh",
                    content = "बनी-ठनी चित्रकला किस शैली से सम्बंधित है?",
                    subject = "Art & Culture",
                    category = "Art",
                    upvotes = 120,
                    commentCount = 25,
                    verifiedAnswer = "किशनगढ़ शैली (नागरीदास के समय)।"
                )
            )
            communityPostRepository.saveAll(posts)
            println("DataSeeder: Seeded initial community posts.")
        }
    }

    private fun getOrCreateUser(mobile: String, name: String): com.rajasthanexams.backend.model.User {
        val existingUser = userRepository.findByMobile(mobile)
        return if (existingUser.isPresent) {
            existingUser.get()
        } else {
            val newUser = com.rajasthanexams.backend.model.User(
                mobile = mobile,
                name = name,
                role = com.rajasthanexams.backend.model.Role.STUDENT
            )
            userRepository.save(newUser)
        }
    }
}
