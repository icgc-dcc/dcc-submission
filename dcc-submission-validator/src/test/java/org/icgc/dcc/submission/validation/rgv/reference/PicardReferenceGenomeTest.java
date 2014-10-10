package org.icgc.dcc.submission.validation.rgv.reference;

import static org.assertj.core.api.Assertions.assertThat;
import net.sf.picard.PicardException;

import org.junit.Test;

public class PicardReferenceGenomeTest {

  /**
   * @see http://genome.ucsc.edu/cgi-bin/hgGateway
   * <p>
   * Use chromosome, start, end, reference to check
   */
  private final String[] baseCorrect = new String[] { "21", "33031597", "33031597", "G" };
  private final String[] baseWrong = new String[] { "21", "33031597", "33031597", "C" };
  private final String[] basesCorrect = new String[] { "8", "50000", "50005", "CTAAGA" };
  private final String[] basesWrong = new String[] { "8", "50000", "50005", "AGAATC" };

  PicardReferenceGenome genome = new PicardReferenceGenome("/tmp/GRCh37.fasta");

  @Test
  public void testSingleSequenceCorrect() {
    String ref = genome.getSequence(baseCorrect[0], baseCorrect[1], baseCorrect[2]);
    assertThat(ref).isEqualTo(baseCorrect[3]);
  }

  @Test
  public void testSingleSequenceIncorrect() {
    String ref = genome.getSequence(baseWrong[0], baseWrong[1], baseWrong[2]);
    assertThat(ref).isNotEqualTo(baseWrong[3]);
  }

  @Test
  public void testLongSequenceCorrect() {
    String ref = genome.getSequence(basesCorrect[0], basesCorrect[1], basesCorrect[2]);
    assertThat(ref).isEqualTo(basesCorrect[3]);
  }

  @Test
  public void testLongSequenceInCorrect() {
    String ref = genome.getSequence(basesWrong[0], basesWrong[1], basesWrong[2]);
    assertThat(ref).isNotEqualTo(basesWrong[3]);
  }

  @Test(expected = PicardException.class)
  public void testSequenceOutOfRange() {
    String chromosome = "9";
    String start = "1135797205";
    String end = "1135797205";
    genome.getSequence(chromosome, start, end);
  }

}
