
import groovy.util.GroovyTestCase

class PokerHandTests extends GroovyTestCase {
    void testHand_evaluate() {
        def cases = [
            ['Jd 10d Ad Qd Kd', 'Royal flush, diamonds'],
            ['Jd 10d 9d Qd Kd', 'King-high straight flush, diamonds'],
            ['9c 7c Jc 10c 8c', 'Jack-high straight flush, clubs'],
            ['9s 4c 9h 9c 9d', 'Four of a kind, nines'],
            ['Qs Qh Qc 7h Qd', 'Four of a kind, queens'],
            ['Qd Qc 7s 7h Qh', 'Full house, queens full of sevens'],
            ['Qd Qc As Ah Qh', 'Full house, queens full of aces'],
            ['Jh 2h 5h Ah 7h', 'Ace-high flush, hearts'],
            ['2s 3s 4s 5s 7s', 'Seven-high flush, spades'],
            ['6d 7h 8s 9d 10c', 'Ten-high straight'],
            ['Ad 2c 3d 4h 5s', 'Five-high straight'],
            ['10h Jh Qs Kd Ac', 'Ace-high straight'],
            ['Jd 10d Ad Qd Kc', 'Ace-high straight'],
            ['Jd 10d 9d Qd Ks', 'King-high straight'],
            ['2c 2s 2h 3d 4s', 'Three of a kind, twos'],
            ['5c As 7d Ah Ad', 'Three of a kind, aces'],
            ['2c 6d 4s 6s 2h', 'Two pair, twos and sixes'],
            ['Ks Kd Jd Jh Qs', 'Two pair, jacks and kings'],
            ['2c 4d Js 7s 4h', 'One pair, fours'],
            ['2h 3c 7s 4d 9d', 'High card, nine of diamonds'],
            ['2c 4d Js 7s 10h', 'High card, jack of spades'],

            // Exercise examples
            ['Ah As 10c 7d 6s', 'One pair, aces'],
            ['Kh Kc 3s 3h 2d', 'Two pair, threes and kings'],
            ['Kh Qh 6h 2h 9h', 'King-high flush, hearts'],
            ['3s Kc 3h 2d Kh', 'Two pair, threes and kings'], // shuffled
            ['6h Qh 2h Kh 9h', 'King-high flush, hearts'] // shuffled
        ]

        cases.each {
            runEvaluateTest(it[0], it[1])
        }
    }

    private void runEvaluateTest(String hand, String expected) {
        def h = new PokerHand(hand)
        assertEquals(expected, h.evaluate())
    }

    /**
     * Hand tests
     */
    void testHand_getHighCard() {
        // 2h 3c 7s 4d 9d (High card, nine of diamonds)
        def h = new PokerHand('2h 3c 7s 4d 9d')
        assertToString(h.highCard, 'Card(nine of diamonds)')

        // 2c 4d Js 7s 10h (High card, jack of spades)
        h = new PokerHand('2c 4d Js 7s 10h')
        assertToString(h.highCard, 'Card(jack of spades)')
    }

    void testHand_isAllOneSuit() {
        // Jh 2h 5h Ah 7h (Ace-high flush, hearts)
        def h = new PokerHand('Jh 2h 5h Ah 7h')
        assert h.isAllOneSuit() == true

        // Jh 2h 5h Ad 7h (no flush)
        h = new PokerHand('Jh 2h 5h Ad 7h')
        assert h.isAllOneSuit() == false
    }

    void testHand_isInConsecutiveOrder() {
        // 6d 7h 8s 9d 10c (Ten-high straight)
        def h = new PokerHand('6d 7h 8s 9d 10c')
        assert h.isInConsecutiveOrder() == true

        // 10h Jh Qs Kd Ac (Ace-high straight)
        h = new PokerHand('10h Jh Qs Kd Ac')
        assert h.isInConsecutiveOrder() == true

        // Ad 2c 3d 4h 5s (Five-high straight)
        h = new PokerHand('Ad 2c 3d 4h 5s')
        assert h.isInConsecutiveOrder() == true

        // Kd 2c 3d 4h 5s (no straight)
        h = new PokerHand('Ad 2c 3d 4h 6s')
        assert h.isInConsecutiveOrder() == false

        // 2c 3d 4h 5s 7d (no straight)
        h = new PokerHand('Ad 2c 3d 4h 6s')
        assert h.isInConsecutiveOrder() == false

        // Ad 2c 3d 4h 6s (no straight)
        h = new PokerHand('Ad 2c 3d 4h 6s')
        assert h.isInConsecutiveOrder() == false
    }

    void testHand_getMaxRankCount() {
        // Qs Qh Qc 7h Qd (Four of a kind, queens)
        def h = new PokerHand('Qs Qh Qc 7h Qd')
        assert h.maxRankCount == 4

        // 2c 2s 2h 3d 4s (Three of a kind twos)
        h = new PokerHand('2c 2s 2h 3d 4s')
        assert h.maxRankCount == 3

        // 2c 4d Js 7s 4h (One pair, fours)
        h = new PokerHand('2c 4d Js 7s 4h')
        assert h.maxRankCount == 2

        // 2h 3c 7s 4d 9d (High card, nine of diamonds)
        h = new PokerHand('2h 3c 7s 4d 9d')
        assert h.maxRankCount == 1
    }

    void testHand_failures() {
        def h
        shouldFail(InvalidHandException) {
            h = new PokerHand() // null
        }
        shouldFail(InvalidHandException) {
            h = new PokerHand('') // empty
        }
        shouldFail(InvalidCardException) {
            h = new PokerHand('2h 3c 3s 4d 0s') // spot check on invalid card
        }
        shouldFail(InvalidHandException) {
            h = new PokerHand('2h 3c 3s 4d') // missing card
        }
        shouldFail(InvalidHandException) {
            h = new PokerHand('2h 3c 3s 4d 5h 6d') // extra card
        }
        def cause = shouldFail(InvalidHandException) {
            h = new PokerHand('2h 3c 3s As As') // no cheating
        }
        assertEquals('No cheating!', cause)
    }


    /**
     * Card tests
     */
    void testCard_parseInputSuccess() {
        assertToString(new Card('2c'), 'Card(two of clubs)')
        assertToString(new Card('7d'), 'Card(seven of diamonds)')
        assertToString(new Card('10h'), 'Card(ten of hearts)')
        assertToString(new Card('As'), 'Card(ace of spades)')
    }

    void testCard_compareTo_ranks() {
        // numeric ranks
        def c1 = new Card('3c')
        def c2 = new Card('3c')
        assert c1 == c2

        c2 = new Card('7c')
        assert c1 < c2
        assert c2 > c1

        // face cards
        c1 = new Card('Js')
        c2 = new Card('Qs')
        def c3 = new Card('Ks')
        def c4 = new Card('As')
        assert c1 < c2
        assert c2 < c3
        assert c3 < c4

        // ace always outranks two, despite ace-to-five straight
        c1 = new Card('2h')
        c2 = new Card('Ah')
        assert c1 < c2
    }

    void testCard_compareTo_suits() {
        def c1 = new Card('7c')
        def c2 = new Card('7d')
        def c3 = new Card('7h')
        def c4 = new Card('7s')
        assert c1 < c2
        assert c2 < c3
        assert c3 < c4
    }

    void testCard_parseInputFailure() {
        def c
        shouldFail(InvalidCardException) {
            c = new Card() // null
        }
        shouldFail(InvalidCardException) {
            c = new Card('')
        }
        shouldFail(InvalidCardException) {
            c = new Card('2') // missing info
        }
        shouldFail(InvalidCardException) {
            c = new Card('d') // missing info
        }
        shouldFail(InvalidCardException) {
            c = new Card('7 d') // no spaces
        }
        shouldFail(InvalidCardException) {
            c = new Card('7#d') // garbage
        }
        shouldFail(InvalidCardException) {
            c = new Card('2.3d') // no floats
        }
        shouldFail(InvalidCardException) {
            c = new Card('0d') // correct format, invalid rank
        }
        shouldFail(InvalidCardException) {
            c = new Card('11d') // invalid numeric rank
        }
        shouldFail(InvalidCardException) {
            c = new Card('Th') // invalid face card
        }
        shouldFail(InvalidCardException) {
            c = new Card('jh') // enforce upper case for face cards
        }
        shouldFail(InvalidCardException) {
            c = new Card('2S') // enforce lower case for suits
        }
        shouldFail(InvalidCardException) {
            c = new Card('6q') // invalid suit
        }
        def cause = shouldFail(InvalidCardException) {
            c = new Card('13p') // invalid suit fails first
        }
        assertEquals('Invalid suit', cause)
    }

}
