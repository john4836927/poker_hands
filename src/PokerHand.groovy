/**
 * Class for evaluating a poker hand.  Card representation is a two character
 * string.  First character is the rank (a number or face card). Second is the
 * suit.
 *
 * Rank: 2-10, J, Q, K, A
 * Suit: c, d, h, s
 *
 * Examples:
 *
 * Ah As 10c 7d 6s (Pair of Aces)
 * Kh Kc 3s 3h 2d (2 Pair)
 * Kh Qh 6h 2h 9h (Flush)
 */
class PokerHand {
    private cards = []
    private ranks = null

    /**
     * Takes a string with space-delimited card representations, parses
     * it into a poker hand, and makes it available for evaluation.
     *
     * @param hand hand represented as string
     * @throws InvalidHandException
     * @throws InvalidCardException
     */
    public PokerHand(String s) {
        if (!s) {
            throw new InvalidHandException('Missing hand input string')
        }

        // parse input into cards
        def tmp = s.tokenize()
        tmp.each {
            cards << new Card(it)
        }
        cards.sort()
        validate()
    }

    private validate() {
        if (cards.size() != 5) {
            throw new InvalidHandException('Invalid hand size')
        }
        def tmp = cards.unique()
        if (tmp.size() < 5) {
            throw new InvalidHandException('No cheating!')
        }
    }

    /**
     * Evaluate hand and return a description of its rank
     *
     * @return hand description
     */
    public String evaluate() {
        def e = new HandEvaluator(this)
        return e.result
    }

    /**
     * @return highest ranking card in the hand
     */
    public Card getHighCard() {
        // cards is always sorted
        return cards[4]
    }

    /**
     * @return true if all cards in the hand are the same suit
     */
    public boolean isAllOneSuit() {
        def suits = [:]
        cards.each {
            if (suits[it.suit] == null) {
                suits[it.suit] = 1
            }
        }
        return suits.size() == 1
    }

    /**
     * Used to check for a straight or straight flush.
     *
     * @return true if card ranks are in consecutive order
     */
    public boolean isInConsecutiveOrder() {
        def inOrder = true
        int idx = 0
        int stop = cards.size() - 1
        while (idx < stop) {
            // rank of next must be one more than the current
            if ((cards[idx].rank + 1) != cards[idx+1].rank) {
                // special case -- ace can be in order below a two
                if ( !((idx + 1) == stop && cards[0].rank == 2 && cards[idx+1].rank == Card.ACE_RANK_VALUE) ) {
                    inOrder = false
                    break
                }
            }
            ++idx
        }
        return inOrder
    }

    /**
     * Used to determine three/four of a kind, or pairs.
     *
     * @return largest number of same-rank cards
     */
    private int getMaxRankCount() {
        def max = getRankCounts().max{ it.value }
        return max.value
    }

    /**
     * Returns map with card rank and the number of cards of that rank
     * in the hand.  e.g. an aces over sevens full house produces [A:3, 7:2]
     *
     * @return
     */
    public Map getRankCounts() {
        if (ranks == null) {
            ranks = [:]
            cards.each {
                if (ranks[it.rank] == null) {
                    ranks[it.rank] = 0
                }
                ranks[it.rank]++
            }
        }
        return ranks
    }

    /**
     * Returns a list of Card objects for the given card rank count. 3 and 4 can
     * return a single element list, 2 can return a list of one or two
     * elements (one or two pair).
     *
     * @param rankCount
     * @return
     */
    public List getCardsByRankCount(int rankCount) {
        def rCounts = getRankCounts()

        def rankList = []
        rCounts.findAll{ it.value == rankCount }.each{ rankList << it.key }

        def cardList = []
        rankList.each {r->
            cardList << cards.find{ it.rank == r }
        }
        return cardList
    }

    public String toString() {
        return 'PokerHand(' + cards.join(', ') + ')'
    }
}

class InvalidHandException extends Exception {
    public InvalidHandException(String s) {
        super(s)
    }
}

/**
 * Class with hand evaluation logic
 *
 * Hand definitions, see http://www.pokerlistings.com/poker-hand-ranking
 * Royal Flush
 * A straight from a ten to an ace with all five cards of the same suit. In poker all suits are ranked equally.
 * Straight Flush
 * Any straight with all five cards of the same suit.
 * Four of a Kind
 * Any four cards of the same rank. If two players share the same Four of a Kind (on the board), the bigger fifth card (the "kicker") decides who wins the pot.
 * Full House
 * Any three cards of the same rank together with any two cards of the same rank. Our example shows "Aces full of Kings" and it is a bigger full house than "Kings full of Aces."
 * Flush
 * Any five cards of the same suit (not consecutive). The highest card of the five determines the rank of the flush. Our example shows an Ace-high flush, which is the highest possible.
 * Straight
 * Any five consecutive cards of different suits. Aces can count as either a high or a low card. Our example shows a five-high straight, which is the lowest possible straight.
 * Three of a Kind
 * Any three cards of the same rank. Our example shows three-of-a-kind Aces, with a King and a Queen as side cards - the best possible three of a kind.
 * Two Pair
 * Any two cards of the same rank together with another two cards of the same rank. Our example shows the best possible two-pair, Aces and Kings. The highest pair of the two determines the rank of the two-pair.
 * One Pair
 * Any two cards of the same rank. Our example shows the best possible one-pair hand.
 * High Card
 * Any hand not in the above-mentioned hands. Our example shows the best possible high-card hand.
 */
class HandEvaluator {
    private hand

    public HandEvaluator(PokerHand h) {
        hand = h
    }

    /**
     * Returns human-readable rank for poker hand
     *
     * @return
     */
    public String getResult() {
        def result
        def maxRankCount = hand.maxRankCount

        if (maxRankCount == 4) {
            // if has 4 -- 4 of a kind
            def cardList = hand.getCardsByRankCount(4)
            result = 'Four of a kind, ' + cardList[0].rankPlural

        } else if (maxRankCount == 3) {
            result = getThreeCountResult()

        } else if (hand.isAllOneSuit()) {
            result = getAllOneSuitResult()

        } else if (hand.isInConsecutiveOrder()) {
            // if is consecutive order -- straight
            result = getConsecutiveOrderResult()

        } else if (maxRankCount == 2) {
            result = getPairResult()

        } else {
            // default -- high card
            def c = hand.highCard
            result = 'High card, ' + c.rankString + ' of ' + c.suitString
        }
        return result
    }

    /**
     * Evaluation logic for full house and three of a kind
     *
     * @return
     */
    private String getThreeCountResult() {
        def result
        def threeList = hand.getCardsByRankCount(3)
        def pairList = hand.getCardsByRankCount(2)

        // if has pair -- full house
        if (pairList.size() == 1) {
            result = 'Full house, ' + threeList[0].rankPlural + ' full of ' + pairList[0].rankPlural
        // else -- 3 of a kind
        } else {
            result = 'Three of a kind, ' + threeList[0].rankPlural
        }
        return result
    }

    /**
     * Evaluation logic for flush, straight flush, and royal flush
     *
     * @return
     */
    private String getAllOneSuitResult() {
        def result
        def high = hand.highCard

        // if is consecutive order
        if (hand.isInConsecutiveOrder()) {
            // if high card is ace -- royal flush
            if (high.rank == Card.ACE_RANK_VALUE) {
                result = 'Royal flush'
            // else -- straight flush
            } else {
                result = high.rankString.capitalize() + '-high straight flush'
            }
        // else -- flush
        } else {
            // high card is convenient way to get suit for flush hand
            result = high.rankString.capitalize() + '-high flush'
        }
        result += ', ' + high.suitString
        return result
    }

    /**
     * Evaluation logic for straight, except straight and royal flush
     *
     * @return
     */
    private String getConsecutiveOrderResult() {
        // Need to catch special case of ace switching to a one
        def high
        if (hand.highCard.rank == Card.ACE_RANK_VALUE && hand.cards[0].rank == 2) {
            high = hand.cards[3]
        } else {
            high = hand.highCard
        }
        return high.rankString.capitalize() + '-high straight'
    }

    /**
     * Evaluation logic for one and two pair
     *
     * @return
     */
    private String getPairResult() {
        def result

        // List of cards for one or both pairs
        def cardList = hand.getCardsByRankCount(2)

        // if has 2 x 2 -- two pair
        if (cardList.size() == 2) {
            result = 'Two pair, '
        // else -- one pair
        } else {
            result = 'One pair, '
        }

        // Add card ranks to result string
        def rankStrings = []
        cardList.each {c->
            rankStrings << c.rankPlural
        }
        result += rankStrings.join(' and ')
        return result
    }
}

/**
 * Stores rank and suit for a card
 */
class Card implements Comparable<Card> {
    // For now, only define ace face value
    static ACE_RANK_VALUE = 14
    private rank
    private suit
    private static suitStrings = [
        's': 'spades',
        'h': 'hearts',
        'd': 'diamonds',
        'c': 'clubs'
        ]
    private static faceCardValues = ['J': 11, 'Q': 12, 'K': 13, 'A': ACE_RANK_VALUE]
    private static rankStrings = ['two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'jack', 'queen', 'king', 'ace']

    /**
     * Take string representation of a card, parse into rank and suit.
     * e.g.
     *  - 'Ah': ace of hearts
     *  - '10c': ten of clubs
     *
     * @param s
     * @throws InvalidCardException
     */
    public Card(String s) {
        validateBasics(s)
        extractSuit(s)
        extractRank(s)
    }

    private validateBasics(String s) {
        if (!s) {
            // null or empty
            throw new InvalidCardException('Missing card string')
        }
        if ((s ==~ /^[0-9A-Za-z]+$/) == false) {
            // basic sanity check
            throw new InvalidCardException('Invalid card string characters')
        }
        if (s.size() < 2) {
            throw new InvalidCardException('Invalid card string')
        }
    }

    /**
     * Extract suit from constructor input string.  Sets suit.
     *
     * @param s string representation of card
     */
    private extractSuit(String s) {
        // last char as suit, remainder is rank
        suit = s[-1]
        if (suitStrings[suit] == null) {
            throw new InvalidCardException('Invalid suit')
        }
    }

    /**
     * Extract rank from constructor input string.  Sets rank.
     *
     * @param s string representation of card
     */
    private extractRank(String s) {
        def tmp = s[0..-2]
        try {
            rank = tmp.toInteger()
            // numeric rank boundaries
            if (rank < 2 || rank > 10) {
                throw new InvalidCardException('Invalid numeric rank')
            }
        } catch (NumberFormatException e) {
            rank = faceCardValues[tmp]
            if (rank == null) {
                throw new InvalidCardException('Invalid face card rank')
            }
        }
    }

    /**
     * Compare Card objects for sorting
     *
     * @param other Card to compare
     * @return
     */
    public int compareTo(Card other) {
        if (rank == other.rank) {
            return suit.compareTo(other.suit)
        } else {
            return rank.compareTo(other.rank)
        }
    }

    public String toString() {
        return 'Card(' + getRankString() + ' of ' + getSuitString() + ')'
    }

    /**
     * Returns string representation of rank
     *
     * @return
     */
    public String getRankString() {
        return rankStrings[rank-2]
    }

    /**
     * Returns pluralized string of rank
     *
     * @return
     */
    public String getRankPlural() {
        if (rank == 6) {
            return getRankString() + 'es'
        } else {
            return getRankString() + 's'
        }
    }

    /**
     * Returns string representation of suit
     *
     * @return
     */
    public String getSuitString() {
        return suitStrings[suit]
    }

}

class InvalidCardException extends Exception {
    public InvalidCardException(String s) {
        super(s)
    }
}
