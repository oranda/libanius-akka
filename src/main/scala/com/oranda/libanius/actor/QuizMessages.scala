package com.oranda.libanius.actor

import com.oranda.libanius.model.quizgroup.QuizGroupKey

object QuizMessages {

  sealed trait QuizMessage {
    val userId: UserId
  }

  final case class ScoreSoFar(userId: UserId) extends QuizMessage

  final case class ProduceQuizItem(userId: UserId) extends QuizMessage

  final case class UpdateWithUserResponse(
    userId: UserId,
    quizGroupKey: QuizGroupKey,
    prompt: String,
    correctResponse: String,
    isCorrect: Boolean
  ) extends QuizMessage

  final case class ActivateQuizGroup(
    userId: UserId,
    quizGroupKey: QuizGroupKey,
    singleGroupActiveMode: Boolean
  ) extends QuizMessage

  final case class RemoveQuizItem(
    userId: UserId,
    quizGroupKey: QuizGroupKey,
    prompt: String,
    correctResponse: String
  ) extends QuizMessage

  final case class IsResponseCorrect(
    userId: UserId,
    quizGroupKey: QuizGroupKey,
    prompt: String,
    userResponse: String
  ) extends QuizMessage
}
