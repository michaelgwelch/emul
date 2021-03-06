/*
  compile.c

  (c) 1991-2012 Edward Swartz

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
 */
#include <alloc.h>
#include <stdio.h>
#include <stdlib.h>

typedef	unsigned int	word;
typedef	unsigned char	byte;

word	rom[4096];				// rom image


int	compile(void);

int	main(int argc, char *argv[])
{
	FILE	*romfile;
	word	a;

	if (argc<2)
	{
		printf("COMPILE [TI ROM image]\n"
		       "\n"
		       "Compiles a ROM into 80x86 assembly for use\n"
		       "with TI Emulator! v6.0.\n");
		exit(0);
	}

	romfile=fopen(argv[1],"rb");
	if (romfile==NULL)
	{
		perror("open rom image:");
		exit(1);
	}

	if (fread(rom,1,8192,romfile)!=8192)
	{
		printf("Short file\n");
		exit(1);
	}

	swab((byte *)rom,(byte *)rom,8192);

	fclose(romfile);

	exit(compile());
}


void	initcode(void);
void	endcode(void);
word	decode(word,word*,word*,word*,byte*,byte*,word*,word*);
void	encode(word,word,word,word,word,byte,byte,word,word);


int	compile(void)
{
	word	addr;
	word	newaddr;
	word	op;
	word	s,d,sa,da;
	byte	ts,td;


	initcode();

	addr=0;
	while (addr<8192)
	{
		newaddr=decode(addr,&op,&s,&d,&ts,&td,&sa,&da);
			encode(addr,newaddr,op,s,d,ts,td,sa,da);
		addr=newaddr;
	}

	endcode();

	return	0;
}


void	initcode(void)
{
	fprintf(stdout, "\t.model small\n"
			"\tdosseg\n"
			"\t.code\n"
			"\n"
			"\tinclude\tcompile.inc\n"
			"\n"
			"\t.286\n"
			"\n"
			"\n"
			";\tGenerated by COMPILE.\n"
			"\n"
			"\n"
			"\n");

}


void	endcode(void)
{
	word	addr;
	word	count;

	fprintf(stdout, "\n\tjmp return\n"
			"\n"
			"\n"
			"\torg\t0\n"
			"\n");

	for (addr=0; addr<8192; addr+=32)
	{
		fprintf(stdout,"\tdw");
		for (count=0; count<32; count+=2)
			fprintf(stdout,"%c@%04X",(count ? ',' : '\t'),
						addr+count);
		fprintf(stdout,"\n");
	}
	fprintf(stdout, "\n"
			"\n"
			"\n"
			"end\n");
}

