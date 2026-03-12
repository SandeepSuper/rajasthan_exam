package com.rajasthanexams.backend.service

import com.rajasthanexams.backend.dto.CreateExamRequest
import com.rajasthanexams.backend.dto.CreateQuestionRequest
import com.rajasthanexams.backend.dto.CreateTestRequest
import com.rajasthanexams.backend.model.Exam
import com.rajasthanexams.backend.model.Question
import com.rajasthanexams.backend.model.Test
import com.rajasthanexams.backend.repository.ExamRepository
import com.rajasthanexams.backend.repository.QuestionRepository
import com.rajasthanexams.backend.repository.TestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ContentService(
    private val examRepository: ExamRepository,
    private val testRepository: TestRepository,
    private val questionRepository: QuestionRepository,
    private val testAttemptRepository: com.rajasthanexams.backend.repository.TestAttemptRepository,
    private val purchaseRepository: com.rajasthanexams.backend.repository.PurchaseRepository
) {

    fun createExam(request: CreateExamRequest): Exam {
        val exam = Exam(
            title = request.title,
            category = request.category,
            iconUrl = request.iconUrl,
            languageSupported = request.languageSupported,
            negativeMarks = request.negativeMarks,
            marksPerQuestion = request.marksPerQuestion,
            isPremium = request.isPremium,
            price = request.price,
            discountPercent = request.discountPercent
        )
        return examRepository.save(exam)
    }

    fun updateExam(examId: UUID, request: com.rajasthanexams.backend.dto.UpdateExamRequest): Exam {
        val exam = examRepository.findById(examId).orElseThrow {
            IllegalArgumentException("Exam not found")
        }
        val updatedExam = exam.copy(
            title = request.title ?: exam.title,
            category = request.category ?: exam.category,
            iconUrl = request.iconUrl ?: exam.iconUrl,
            isPremium = request.isPremium ?: exam.isPremium,
            price = request.price ?: exam.price,
            currency = request.currency ?: exam.currency,
            discountPercent = request.discountPercent ?: exam.discountPercent ?: 0
        )
        return examRepository.save(updatedExam)
    }

    fun createTest(request: CreateTestRequest): Test {
        val exam = examRepository.findById(request.examId).orElseThrow {
            IllegalArgumentException("Exam not found")
        }
        val test = Test(
            exam = exam,
            title = request.title,
            type = request.type,
            durationMinutes = request.durationMinutes,
            totalQuestions = request.totalQuestions,
            isPremium = request.isPremium,
            isLive = request.isLive,
            startsAt = request.startsAt,
            endsAt = request.endsAt,
            allowPrevious = request.allowPrevious,
            sectionLock = request.sectionLock,
            showResultImmediately = !request.isLive, // Default false for Live
            marksPerQuestionOverride = request.marksPerQuestion,
            totalMarks = request.totalMarks
        )
        return testRepository.save(test)
    }

    @Transactional
    fun createQuestion(request: CreateQuestionRequest): Question {
        val test = testRepository.findById(request.testId).orElseThrow {
            IllegalArgumentException("Test not found")
        }
        val question = Question(
            test = test,
            textHi = request.textHi,
            textEn = request.textEn,
            optionsHi = request.optionsHi,
            optionsEn = request.optionsEn,
            correctOptionIndex = request.correctOptionIndex,
            solutionHi = request.solutionHi,
            solutionEn = request.solutionEn
        )
        return questionRepository.save(question)
    }

    fun getAllExams(): List<Exam> {
        return examRepository.findAll()
    }

    fun getAllExamsWithCounts(userId: UUID? = null): List<com.rajasthanexams.backend.dto.ExamDto> {
        val purchasedExamIds = if (userId != null) {
            purchaseRepository.findAllByUserId(userId.toString()).map { it.examId }.toSet()
        } else {
            emptySet()
        }

        return examRepository.findAll().map { exam ->
            val count = testRepository.countByExamId(exam.id!!)
            
            // Logic to use specific icon or generated one
            val finalIconUrl = if (!exam.iconUrl.isNullOrEmpty()) {
                exam.iconUrl
            } else {
                 val encodedName = java.net.URLEncoder.encode(exam.title, "UTF-8")
                "https://ui-avatars.com/api/?name=$encodedName&background=random&size=256&bold=true&rounded=true"
            }

            // Check if purchased
            val isPurchased = purchasedExamIds.contains(exam.id.toString())

            com.rajasthanexams.backend.dto.ExamDto(
                id = exam.id,
                title = exam.title,
                category = exam.category,
                iconUrl = finalIconUrl,
                testCount = count,
                languageSupported = exam.languageSupported,
                marksPerQuestion = exam.marksPerQuestion ?: 1.0,
                isPremium = exam.isPremium ?: false,
                price = exam.price ?: 0.0,
                discountPercent = exam.discountPercent ?: 0,
                isPurchased = isPurchased
            )
        }
    }

    fun getTests(examId: UUID): List<Test> {
        return testRepository.findByExamId(examId)
    }

    fun getLiveTests(): List<Test> {
        // Return tests that end in the future (Active or Upcoming)
        return testRepository.findByIsLiveTrueAndEndsAtAfter(java.time.LocalDateTime.now())
    }

    fun getTestById(testId: UUID): Test {
        return testRepository.findById(testId).orElseThrow { IllegalArgumentException("Test not found") }
    }

    fun hasUserAttemptedTest(testId: UUID, userId: UUID): Boolean {
        return testAttemptRepository.existsByTestIdAndUserId(testId, userId)
    }

    fun getAttemptedTestIds(userId: UUID): Set<UUID> {
        return testAttemptRepository.findAttemptedTestIdsByUserId(userId).toSet()
    }

    fun getQuestions(testId: UUID): List<Question> {
        return questionRepository.findByTestId(testId)
    }

    @Transactional
    fun importQuestions(testId: UUID, file: org.springframework.web.multipart.MultipartFile): Int {
        val test = testRepository.findById(testId).orElseThrow {
            IllegalArgumentException("Test not found with ID: $testId")
        }

        // Parse CSV
        java.io.InputStreamReader(file.inputStream).use { reader ->
            val csvReader = com.opencsv.CSVReader(reader)
            val rows = csvReader.readAll()
            
            // Skip Header if present (simple check: if first row has "textEn")
            val startIndex = if (rows.isNotEmpty() && rows[0][0].equals("textEn", ignoreCase = true)) 1 else 0
            
            val questions = mutableListOf<Question>()
            
            for (i in startIndex until rows.size) {
                val row = rows[i]
                if (row.size < 13) continue // specific to our format check

                // Format:
                // 0: textEn, 1: textHi
                // 2: optA_En, 3: optB_En, 4: optC_En, 5: optD_En
                // 6: optA_Hi, 7: optB_Hi, 8: optC_Hi, 9: optD_Hi
                // 10: correctIndex
                // 11: solutionEn, 12: solutionHi

                val optionsEn = listOf(row[2], row[3], row[4], row[5])
                val optionsHi = listOf(row[6], row[7], row[8], row[9])
                
                val q = Question(
                    test = test,
                    textEn = row[0],
                    textHi = row[1],
                    optionsEn = optionsEn,
                    optionsHi = optionsHi,
                    correctOptionIndex = row[10].toIntOrNull() ?: 0,
                    solutionEn = row[11],
                    solutionHi = row[12],
                    subject = if (row.size > 13 && row[13].isNotBlank()) row[13] else null,
                    marksPerQuestion = if (row.size > 14 && row[14].isNotBlank()) row[14].toDoubleOrNull() else null,
                    negativeMarks = if (row.size > 15 && row[15].isNotBlank()) row[15].toDoubleOrNull() else null
                )
                questions.add(q)
            }
            
            questionRepository.saveAll(questions)
            return questions.size
        }
    }

    @Transactional
    fun createSampleLiveTest() {
        val exams = examRepository.findAll()
        if (exams.isEmpty()) throw IllegalStateException("No exams found to attach test to")
        
        val exam = exams.first()
        val now = java.time.LocalDateTime.now()
        
        val test = Test(
            exam = exam,
            title = "Live Mock Test: Rajasthan GK Special",
            type = com.rajasthanexams.backend.model.TestType.MOCK,
            durationMinutes = 60,
            totalQuestions = 50,
            isPremium = false,
            isLive = true,
            startsAt = now.plusMinutes(15), // Starts in 15 mins
            endsAt = now.plusHours(2),
            allowPrevious = false,
            sectionLock = true,
            showResultImmediately = false,
            marksPerQuestionOverride = 2.0,
            totalMarks = 100.0 // Explicitly setting 100 (50 questions * 2)
        )
        testRepository.save(test)
    }
}
